import chisel3.experimental._
import chisel3._
import chisel3.util._
import decode._
import os.read
import execute._
import utils._
import firrtl.seqCat

class InstructionExecuteUnit extends Module {
  val decodeIn = IO(Flipped(new DecodeOut()))
  val memIO    = IO(Flipped(new MemIO()))
  val regIO    = IO(Flipped(new RegisterFileIO()))
  val csrIn    = IO(Input(UInt(64.W)))

  // val decodeIn = RegNext(in.bits, DecodeOut.default)
  val controlIn = decodeIn.control
  val dataIn    = decodeIn.data

  val alu = Module(new ALU)

  // TODO: impl this
  // in.ready := true.B

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
  regIO.dnpc := Mux(pcBranch.asBool, dnpcAlter, snpc)
  val regwdata = MuxLookup(
    controlIn.regwritemux,
    DontCare,
    EnumSeq(
      RegWriteMux.alu -> alu.io.out,
      RegWriteMux.snpc -> snpc,
      RegWriteMux.mem -> memIO.rdata,
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
  memIO.clock      := clock
  memIO.addr       := alu.io.out
  memIO.isRead     := controlIn.memmode === MemMode.read.asUInt || controlIn.memmode === MemMode.readu.asUInt
  memIO.isUnsigned := controlIn.memmode === MemMode.readu.asUInt
  memIO.enable     := controlIn.memmode =/= MemMode.no.asUInt
  // TODO
  memIO.len := MuxLookup(
    controlIn.memlen,
    1.U,
    EnumSeq(
      MemLen.one -> 1.U,
      MemLen.two -> 2.U,
      MemLen.four -> 4.U,
      MemLen.eight -> 8.U
    )
  )
  memIO.wdata := src2
  // blackBoxHalt
  val blackBox = Module(new BlackBoxHalt);
  blackBox.io.halt     := controlIn.goodtrap
  blackBox.io.bad_halt := controlIn.badtrap
}
