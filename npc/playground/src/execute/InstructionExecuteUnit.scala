import chisel3.experimental._
import chisel3._
import chisel3.util._
import decode._
import os.read
import execute._
import utils._
import firrtl.seqCat
import firrtl.transforms.MustDeduplicateTransform

class InstructionExecuteUnit extends Module {
  val decodeIn = IO(Flipped(new DecodeOut()))
  val memAxiM  = IO(MemAxiLite())
  val regIO    = IO(Flipped(new RegisterFileIO()))
  val csrIn    = IO(Input(UInt(64.W)))

  val controlIn = decodeIn.control
  val dataIn    = decodeIn.data

  val alu = Module(new ALU)

  val memOut = Wire(UInt(64.W))

  val busy = false.B

  val postValid  = RegNext(decodeIn.valid)
  val firstValid = decodeIn.valid && !postValid
  // regIO
  val src1 = Wire(UInt(64.W))
  val src2 = Wire(UInt(64.W))
  regIO.raddr0 := dataIn.src1
  regIO.raddr1 := dataIn.src2
  regIO.waddr  := Mux(controlIn.regwrite, dataIn.dst, 0.U)
  val snpc = regIO.pc + 4.U
  val pcBranch = MuxLookup(
    controlIn.pcaddrsrc,
    false.B,
    EnumSeq(
      PCAddrSrc.aluzero -> alu.signalIO.isZero,
      PCAddrSrc.aluneg -> alu.signalIO.isNegative,
      PCAddrSrc.alunotneg -> !alu.signalIO.isNegative,
      PCAddrSrc.alunotzero -> !alu.signalIO.isZero,
      PCAddrSrc.alunotcarryandnotzero -> (!alu.signalIO.isCarry && !alu.signalIO.isZero),
      PCAddrSrc.alucarryorzero -> (alu.signalIO.isCarry || alu.signalIO.isZero),
      PCAddrSrc.zero -> false.B,
      PCAddrSrc.one -> true.B
    )
  )
  val dnpcAddSrc = MuxLookup(
    controlIn.pcsrc,
    regIO.pc,
    EnumSeq(
      PcSrc.pc -> regIO.pc,
      PcSrc.src1 -> src1
    )
  )
  val dnpcAlter = MuxLookup(
    controlIn.pccsr,
    dnpcAddSrc,
    EnumSeq(
      PcCsr.origin -> (dnpcAddSrc + dataIn.imm),
      PcCsr.csr -> csrIn
    )
  )
  regIO.dnpc := Mux(firstValid, Mux(pcBranch.asBool, dnpcAlter, snpc), regIO.pc)
  val regwdata = MuxLookup(
    controlIn.regwritemux,
    DontCare,
    EnumSeq(
      RegWriteMux.alu -> alu.io.out,
      RegWriteMux.snpc -> snpc,
      RegWriteMux.mem -> memOut,
      RegWriteMux.aluneg -> Utils.zeroExtend(alu.signalIO.isNegative, 1, 64),
      RegWriteMux.alunotcarryandnotzero -> Utils
        .zeroExtend(!alu.signalIO.isCarry && !alu.signalIO.isZero, 1, 64),
      RegWriteMux.csr -> csrIn
    )
  )
  regIO.wdata := Mux(controlIn.regwsext, Utils.signExtend(regwdata.asUInt, 32), regwdata)

  src1 :=
    Mux(
      controlIn.srccast1,
      Utils.cast(regIO.out0, 32, 64),
      regIO.out0
    )
  src2 :=
    Mux(
      controlIn.srccast2,
      Utils.cast(regIO.out1, 32, 64),
      regIO.out1
    )

  // alu
  alu.io.inA := MuxLookup(
    controlIn.alumux1,
    DontCare,
    EnumSeq(
      AluMux1.pc -> regIO.pc,
      AluMux1.src1 -> src1,
      AluMux1.zero -> 0.U
    )
  )
  alu.io.inB := MuxLookup(
    controlIn.alumux2,
    DontCare,
    EnumSeq(
      AluMux2.imm -> dataIn.imm,
      AluMux2.src2 -> src2
    )
  )
  alu.io.opType := AluMode.apply(controlIn.alumode)

  // mem
  val memlen = MuxLookup(
    controlIn.memlen,
    1.U,
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

  val memIsRead     = controlIn.memmode === MemMode.read.asUInt || controlIn.memmode === MemMode.readu.asUInt
  val shouldMemWork = controlIn.memmode =/= MemMode.no.asUInt

  val waitReq :: waitRes :: idle :: other = Enum(4)

  val memStatus = RegInit(idle)
  memStatus := FSM(
    memStatus,
    List(
      (idle, shouldMemWork, waitReq),
      (waitReq, Mux(memIsRead, memAxiM.AR.fire, memAxiM.AW.fire && memAxiM.W.fire), waitRes),
      (waitRes, Mux(memIsRead, memAxiM.R.fire, memAxiM.B.fire), idle)
    )
  )

  memAxiM.AR.valid     := memStatus === waitReq && memIsRead
  memAxiM.AR.bits.addr := alu.io.out
  memAxiM.AR.bits.id   := 0.U
  memAxiM.AR.bits.prot := 0.U
  memAxiM.R.ready      := memStatus === waitRes && memIsRead
  memAxiM.AW.valid     := memStatus === waitReq && !memIsRead
  memAxiM.AW.bits.addr := alu.io.out
  memAxiM.AW.bits.id   := 0.U
  memAxiM.AW.bits.prot := 0.U
  memAxiM.W.valid      := memStatus === waitReq && !memIsRead
  memAxiM.W.bits.data  := src2
  memAxiM.W.bits.strb  := memMask
  memAxiM.B.ready      := memStatus === waitRes
  memOut := Mux(
    controlIn.memmode === MemMode.read.asUInt,
    Utils.signExtend(memAxiM.R.bits.data.asUInt, memlen << 3),
    Utils.zeroExtend(memAxiM.R.bits.data.asUInt, memlen << 3)
  )

  // blackBoxHalt
  val blackBox = Module(new BlackBoxHalt);
  blackBox.io.halt     := controlIn.goodtrap
  blackBox.io.bad_halt := controlIn.badtrap

  decodeIn.done := !firstValid && memStatus === idle
}
