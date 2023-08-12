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
    Seq(
      PCAddrSrc.aluzero.asUInt -> alu.signalIO.isZero,
      PCAddrSrc.aluneg.asUInt -> alu.signalIO.isNegative,
      PCAddrSrc.alunotneg.asUInt -> !alu.signalIO.isNegative,
      PCAddrSrc.alunotzero.asUInt -> !alu.signalIO.isZero,
      PCAddrSrc.alunotcarryandnotzero.asUInt -> (!alu.signalIO.isCarry && !alu.signalIO.isZero),
      PCAddrSrc.alucarryorzero.asUInt -> (alu.signalIO.isCarry || alu.signalIO.isZero),
      PCAddrSrc.zero.asUInt -> false.B,
      PCAddrSrc.one.asUInt -> true.B
    )
  )
  val dnpcAddSrc = MuxLookup(
    controlIn.pcsrc,
    regIO.pc,
    Seq(
      PcSrc.pc.asUInt -> regIO.pc,
      PcSrc.src1.asUInt -> src1
    )
  )
  val dnpcAlter = MuxLookup(
    controlIn.pccsr,
    dnpcAddSrc,
    Utils.chiselEnumSeq(
      PcCsr.origin -> dnpcAddSrc,
      PcCsr.csr -> csrIn
    )
  )
  regIO.dnpc := Mux(pcBranch, dnpcAddSrc + dataIn.imm, snpc)
  val regwdata = MuxLookup(
    controlIn.regwritemux,
    DontCare,
    Seq(
      RegWriteMux.alu.asUInt -> alu.io.out,
      RegWriteMux.snpc.asUInt -> snpc,
      RegWriteMux.mem.asUInt -> memIO.rdata,
      RegWriteMux.aluneg.asUInt -> Utils.zeroExtend(alu.signalIO.isNegative, 1, 64),
      RegWriteMux.alunotcarryandnotzero.asUInt -> Utils.zeroExtend(!alu.signalIO.isCarry && !alu.signalIO.isZero, 1, 64)
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
    Seq(
      AluMux1.pc.asUInt -> regIO.pc,
      AluMux1.src1.asUInt -> src1,
      AluMux1.zero.asUInt -> 0.U
    )
  )
  alu.io.inB := MuxLookup(
    controlIn.alumux2,
    DontCare,
    Seq(
      AluMux2.imm.asUInt -> dataIn.imm,
      AluMux2.src2.asUInt -> src2
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
    Seq(
      MemLen.one.asUInt -> 1.U,
      MemLen.two.asUInt -> 2.U,
      MemLen.four.asUInt -> 4.U,
      MemLen.eight.asUInt -> 8.U
    )
  )
  memIO.wdata := src2
  // blackBoxHalt
  val blackBox = Module(new BlackBoxHalt);
  blackBox.io.halt     := controlIn.goodtrap
  blackBox.io.bad_halt := controlIn.badtrap
}
