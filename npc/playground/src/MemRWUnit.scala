import chisel3.experimental._
import chisel3._
import chisel3.util._
import decode._
import os.read
import execute._
import utils._

class MemRWIn extends Bundle {
  val debug   = Output(new DebugInfo)
  val data    = Output(new MemDataIn);
  val control = Output(new ExeControlIn);
}

class MemDataIn extends Bundle {
  val src1    = Output(UInt(5.W))
  val src2    = Output(UInt(5.W))
  val dst     = Output(UInt(5.W))
  val imm     = Output(UInt(64.W))
  val alu     = Output(UInt(64.W))
  val signals = new SignalIO()
  val pc      = Input(UInt(64.W))
}

class MemRWUnit extends Module {
  val memIO  = IO(Flipped(new CacheIO(64, 64)))
  val memIn  = IO(Flipped(Decoupled(new MemRWIn())))
  val memOut = IO(Decoupled(new WBIn()))
  // val regIO      = IO(Flipped(new RegisterFileIO()))
  // val csrIn      = IO(Input(UInt(64.W)))
  // val csrControl = IO(Flipped(new CSRFileControl()))

  val memInReg = Reg(new MemRWIn())

  val shouldMemWork = memIn.bits.control.memmode =/= MemMode.no.asUInt
  val memIsRead     = memInReg.control.memmode === MemMode.read.asUInt || memInReg.control.memmode === MemMode.readu.asUInt

  // val shouldWaitALU = !alu.io.out.bits.isImmidiate

  val idle :: waitIn :: waitMemReq :: waitMemRes :: waitOut :: other = Enum(10)

  val memFSM = new FSM(
    idle,
    List(
      (idle, memOut.ready, waitIn),
      (waitIn, memIn.fire && shouldMemWork, waitMemReq),
      (waitIn, memIn.fire && !shouldMemWork, waitOut),
      (waitMemReq, Mux(memIsRead, memIO.readReq.fire, memIO.writeReq.fire), waitMemRes),
      (waitMemRes, Mux(memIsRead, memIO.data.fire, memIO.writeRes.fire), waitOut),
      (waitOut, memOut.fire, idle)
    )
  )
  // regIO
  // val src1 = Wire(UInt(64.W))
  // val src2 = Wire(UInt(64.W))
  // regIO.raddr0 := exeInReg.data.src1
  // regIO.raddr1 := exeInReg.data.src2
  // regIO.waddr  := Mux(exeInReg.control.regwrite && memFSM.willChangeTo(waitPC), exeInReg.data.dst, 0.U)
  // val snpc = regIO.pc + 4.U
  // val pcBranch = MuxLookup(exeInReg.control.pcaddrsrc, false.B)(
  //   EnumSeq(
  //     PCAddrSrc.aluzero -> alu.io.out.bits.signals.isZero,
  //     PCAddrSrc.aluneg -> alu.io.out.bits.signals.isNegative,
  //     PCAddrSrc.alunotneg -> !alu.io.out.bits.signals.isNegative,
  //     PCAddrSrc.alunotzero -> !alu.io.out.bits.signals.isZero,
  //     PCAddrSrc.alunotcarryandnotzero -> (!alu.io.out.bits.signals.isCarry && !alu.io.out.bits.signals.isZero),
  //     PCAddrSrc.alucarryorzero -> (alu.io.out.bits.signals.isCarry || alu.io.out.bits.signals.isZero),
  //     PCAddrSrc.zero -> false.B,
  //     PCAddrSrc.one -> true.B
  //   )
  // )
  // val csrInReg = RegInit(csrIn)
  // csrInReg := Mux(memFSM.willChangeTo(waitPC), csrIn, csrInReg)
  // val dnpcAddSrcReg = RegNext(
  //   MuxLookup(exeInReg.control.pcsrc, regIO.pc)(
  //     EnumSeq(
  //       PcSrc.pc -> regIO.pc,
  //       PcSrc.src1 -> src1
  //     )
  //   )
  // )
  // val dnpcAlter = MuxLookup(exeInReg.control.pccsr, dnpcAddSrcReg)(
  //   EnumSeq(
  //     PcCsr.origin -> (dnpcAddSrcReg + exeInReg.data.imm),
  //     PcCsr.csr -> csrInReg
  //   )
  // )
  // regIO.dnpc := Mux(exeFSM.is(waitPC), Mux(pcBranch.asBool, dnpcAlter, snpc), regIO.pc)
  // val regwdata = MuxLookup(exeInReg.control.regwritemux, alu.io.out.bits.out)(
  //   EnumSeq(
  //     RegWriteMux.alu -> alu.io.out.bits.out,
  //     RegWriteMux.snpc -> snpc,
  //     RegWriteMux.mem -> memOut,
  //     RegWriteMux.aluneg -> Utils.zeroExtend(alu.io.out.bits.signals.isNegative, 1, 64),
  //     RegWriteMux.alunotcarryandnotzero -> Utils
  //       .zeroExtend(!alu.io.out.bits.signals.isCarry && !alu.io.out.bits.signals.isZero, 1, 64),
  //     RegWriteMux.csr -> csrIn
  //   )
  // )
  // regIO.wdata := Mux(exeInReg.control.regwsext, Utils.signExtend(regwdata.asUInt, 32), regwdata)

  // src1 :=
  //   Mux(
  //     exeInReg.control.srccast1,
  //     Utils.cast(regIO.out0, 32, 64),
  //     regIO.out0
  //   )
  // src2 :=
  //   Mux(
  //     exeInReg.control.srccast2,
  //     Utils.cast(regIO.out1, 32, 64),
  //     regIO.out1
  //   )

  // // alu
  // alu.io.in.bits.inA := MuxLookup(exeInReg.control.alumux1, 0.U)(
  //   EnumSeq(
  //     AluMux1.pc -> regIO.pc,
  //     AluMux1.src1 -> src1,
  //     AluMux1.zero -> 0.U
  //   )
  // )
  // alu.io.in.bits.inB := MuxLookup(exeInReg.control.alumux2, 0.U)(
  //   EnumSeq(
  //     AluMux2.imm -> exeInReg.data.imm,
  //     AluMux2.src2 -> src2
  //   )
  // )
  // val res = AluMode.safe(exeInReg.control.alumode)
  // alu.io.in.bits.opType := res._1
  // alu.io.out.ready      := alu.io.out.bits.isImmidiate || exeFSM.is(waitALU)
  // alu.io.in.valid       := exeIn.fire
  // // csr
  // csrControl.csrBehave  := Mux(exeFSM.willChangeTo(waitPC), exeInReg.control.csrbehave, CsrBehave.no.asUInt)
  // csrControl.csrSetmode := Mux(exeFSM.willChangeTo(waitPC), exeInReg.control.csrsetmode, CsrSetMode.origin.asUInt)
  // csrControl.csrSource  := exeInReg.control.csrsource

  // mem
  val memlen = MuxLookup(memInReg.control.memlen, 1.U)(
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
  memAddrReg := Mux(memFSM.willChangeTo(waitMemReq), memInReg.data.alu, memAddrReg)

  memIO.readReq.valid      := memFSM.is(waitMemReq) && memIsRead && shouldMemWork
  memIO.addr               := memAddrReg
  memIO.data.ready         := memFSM.is(waitMemRes) && memIsRead
  memIO.writeReq.valid     := memFSM.is(waitMemReq) && !memIsRead && shouldMemWork
  memIO.writeReq.bits.data := memInReg.data.src2
  memIO.writeReq.bits.mask := memMask
  memIO.writeRes.ready     := memFSM.is(waitMemRes)
  val memOutRaw = MuxLookup(memInReg.control.memlen, memIO.data.bits)(
    EnumSeq(
      MemLen.one -> memIO.data.asUInt(7, 0),
      MemLen.two -> memIO.data.asUInt(15, 0),
      MemLen.four -> memIO.data.asUInt(31, 0),
      MemLen.eight -> memIO.data.asUInt
    )
  )
  val memData = Mux(
    memInReg.control.memmode === MemMode.read.asUInt,
    Utils.signExtend(memOutRaw, memlen << 3),
    Utils.zeroExtend(memOutRaw, memlen << 3)
  )

  memIn.ready := memFSM.is(idle)

  memOut.valid         := memFSM.is(waitOut)
  memOut.bits.debug    := memInReg.debug
  memOut.bits.data     := memInReg.data
  memOut.bits.data.mem := memData
}
