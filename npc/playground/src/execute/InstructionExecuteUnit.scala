import chisel3.experimental._
import chisel3._
import chisel3.util._
import decode._
import os.read
import execute._
import utils._

class ExeDataIn extends Bundle {
  val src1 = Input(UInt(5.W))
  val src2 = Input(UInt(5.W))
  val dst  = Input(UInt(5.W))
  val imm  = Input(UInt(64.W))
}

class ExeIn extends Bundle {
  val data    = Input(new ExeDataIn);
  val control = Input(new ExeControlIn);
}

class InstructionExecuteUnit extends Module {
  val exeIn      = IO(Decoupled(new ExeIn()))
  val memIO      = IO(Flipped(new CacheIO(64, 64)))
  val regIO      = IO(Flipped(new RegisterFileIO()))
  val csrIn      = IO(Input(UInt(64.W)))
  val csrControl = IO(Flipped(new CSRFileControl()))

  val exeInReg = Reg(new ExeIn())

  val alu = Module(new ALU)

  val memOut = Wire(UInt(64.W))

  val shouldMemWork = exeIn.bits.control.memmode =/= MemMode.no.asUInt

  val memIsRead     = exeInReg.control.memmode === MemMode.read.asUInt || exeInReg.control.memmode === MemMode.readu.asUInt
  val shouldWaitALU = !alu.io.out.bits.isImmidiate

  val idle :: waitMemReq :: waitMemRes :: waitPC :: waitALU :: other = Enum(10)

  val exeFSM = new FSM(
    idle,
    List(
      (idle, exeIn.fire && shouldMemWork, waitMemReq),
      (idle, exeIn.fire && shouldWaitALU, waitALU),
      (idle, exeIn.fire, waitPC),
      (waitALU, alu.io.out.fire, waitPC),
      (waitMemReq, Mux(memIsRead, memIO.readReq.fire, memIO.writeReq.fire), waitMemRes),
      (waitMemRes, Mux(memIsRead, memIO.data.fire, memIO.writeRes.fire), waitPC),
      (waitPC, true.B, idle)
    )
  )

  exeInReg := Mux(exeIn.fire, exeIn.bits, exeInReg)
  // regIO
  val src1 = Wire(UInt(64.W))
  val src2 = Wire(UInt(64.W))
  regIO.raddr0 := exeInReg.data.src1
  regIO.raddr1 := exeInReg.data.src2
  regIO.waddr  := Mux(exeInReg.control.regwrite && exeFSM.willChangeTo(waitPC), exeInReg.data.dst, 0.U)
  val snpc = regIO.pc + 4.U
  val pcBranch = MuxLookup(exeInReg.control.pcaddrsrc, false.B)(
    EnumSeq(
      PCAddrSrc.aluzero -> alu.io.out.bits.signals.isZero,
      PCAddrSrc.aluneg -> alu.io.out.bits.signals.isNegative,
      PCAddrSrc.alunotneg -> !alu.io.out.bits.signals.isNegative,
      PCAddrSrc.alunotzero -> !alu.io.out.bits.signals.isZero,
      PCAddrSrc.alunotcarryandnotzero -> (!alu.io.out.bits.signals.isCarry && !alu.io.out.bits.signals.isZero),
      PCAddrSrc.alucarryorzero -> (alu.io.out.bits.signals.isCarry || alu.io.out.bits.signals.isZero),
      PCAddrSrc.zero -> false.B,
      PCAddrSrc.one -> true.B
    )
  )
  val csrInReg = RegInit(csrIn)
  csrInReg := Mux(exeFSM.willChangeTo(waitPC), csrIn, csrInReg)
  val dnpcAddSrcReg = RegNext(
    MuxLookup(exeInReg.control.pcsrc, regIO.pc)(
      EnumSeq(
        PcSrc.pc -> regIO.pc,
        PcSrc.src1 -> src1
      )
    )
  )
  val dnpcAlter = MuxLookup(exeInReg.control.pccsr, dnpcAddSrcReg)(
    EnumSeq(
      PcCsr.origin -> (dnpcAddSrcReg + exeInReg.data.imm),
      PcCsr.csr -> csrInReg
    )
  )
  regIO.dnpc := Mux(exeFSM.is(waitPC), Mux(pcBranch.asBool, dnpcAlter, snpc), regIO.pc)
  val regwdata = MuxLookup(exeInReg.control.regwritemux, alu.io.out.bits.out)(
    EnumSeq(
      RegWriteMux.alu -> alu.io.out.bits.out,
      RegWriteMux.snpc -> snpc,
      RegWriteMux.mem -> memOut,
      RegWriteMux.aluneg -> Utils.zeroExtend(alu.io.out.bits.signals.isNegative, 1, 64),
      RegWriteMux.alunotcarryandnotzero -> Utils
        .zeroExtend(!alu.io.out.bits.signals.isCarry && !alu.io.out.bits.signals.isZero, 1, 64),
      RegWriteMux.csr -> csrIn
    )
  )
  regIO.wdata := Mux(exeInReg.control.regwsext, Utils.signExtend(regwdata.asUInt, 32), regwdata)

  src1 :=
    Mux(
      exeInReg.control.srccast1,
      Utils.cast(regIO.out0, 32, 64),
      regIO.out0
    )
  src2 :=
    Mux(
      exeInReg.control.srccast2,
      Utils.cast(regIO.out1, 32, 64),
      regIO.out1
    )

  // alu
  alu.io.in.bits.inA := MuxLookup(exeInReg.control.alumux1, 0.U)(
    EnumSeq(
      AluMux1.pc -> regIO.pc,
      AluMux1.src1 -> src1,
      AluMux1.zero -> 0.U
    )
  )
  alu.io.in.bits.inB := MuxLookup(exeInReg.control.alumux2, 0.U)(
    EnumSeq(
      AluMux2.imm -> exeInReg.data.imm,
      AluMux2.src2 -> src2
    )
  )
  val res = AluMode.safe(exeInReg.control.alumode)
  alu.io.in.bits.opType := res._1
  alu.io.out.ready      := alu.io.out.bits.isImmidiate || exeFSM.is(waitALU)
  alu.io.in.valid       := exeIn.fire
  // csr
  csrControl.csrBehave  := Mux(exeFSM.willChangeTo(waitPC), exeInReg.control.csrbehave, CsrBehave.no.asUInt)
  csrControl.csrSetmode := Mux(exeFSM.willChangeTo(waitPC), exeInReg.control.csrsetmode, CsrSetMode.origin.asUInt)
  csrControl.csrSource  := exeInReg.control.csrsource

  // mem
  val memlen = MuxLookup(exeInReg.control.memlen, 1.U)(
    EnumSeq(
      MemLen.one -> 1.U,
      MemLen.two -> 2.U,
      MemLen.four -> 4.U,
      MemLen.eight -> 8.U
    )
  )

  val memMask = Cat(
    Fill(4, Mux(memlen > 4.U, 1.U, 0.U)),
    Fill(2, Mux(memlen > 2.U, 1.U, 0.U)),
    Fill(1, Mux(memlen > 1.U, 1.U, 0.U)),
    1.U(1.W)
  )
  val memAddrReg = Reg(UInt(64.W))
  memAddrReg := Mux(exeFSM.willChangeTo(waitMemReq), alu.io.out.bits.out, memAddrReg)

  memIO.readReq.valid      := exeFSM.is(waitMemReq) && memIsRead && shouldMemWork
  memIO.addr               := memAddrReg
  memIO.data.ready         := exeFSM.is(waitMemRes) && memIsRead
  memIO.writeReq.valid     := exeFSM.is(waitMemReq) && !memIsRead && shouldMemWork
  memIO.writeReq.bits.data := src2
  memIO.writeReq.bits.mask := memMask
  memIO.writeRes.ready     := exeFSM.is(waitMemRes)
  val memOutRaw = MuxLookup(exeInReg.control.memlen, memIO.data.bits)(
    EnumSeq(
      MemLen.one -> memIO.data.asUInt(7, 0),
      MemLen.two -> memIO.data.asUInt(15, 0),
      MemLen.four -> memIO.data.asUInt(31, 0),
      MemLen.eight -> memIO.data.asUInt
    )
  )
  memOut := Mux(
    exeInReg.control.memmode === MemMode.read.asUInt,
    Utils.signExtend(memOutRaw, memlen << 3),
    Utils.zeroExtend(memOutRaw, memlen << 3)
  )

  // blackBoxHalt
  val blackBox = Module(new BlackBoxHalt);
  blackBox.io.halt     := exeInReg.control.goodtrap
  blackBox.io.bad_halt := exeInReg.control.badtrap || res._2 === false.B

  exeIn.ready := exeFSM.is(idle)
}
