package execute

import chisel3._
import chisel3.util.MuxLookup
import chisel3.util.Cat
import chisel3.util.Reverse
import chisel3.util.Fill
import decode.AluMode
import firrtl.backends.experimental.smt.Signal
import utils.EnumSeq

object ALUSignalType extends ChiselEnum {
  val isZero, isNegative = Value
}

class FullAdderIO extends Bundle {
  val inA  = Input(UInt(1.W))
  val inB  = Input(UInt(1.W))
  val inC  = Input(UInt(1.W))
  val out  = Output(UInt(1.W))
  val outC = Output(UInt(1.W))
}
class FullAdder extends Module {
  val io = IO(new FullAdderIO())
  io.out  := io.inA ^ io.inB ^ io.inC;
  io.outC := (io.inA & (io.inB ^ io.inC)) | (io.inB & io.inC)
}
class SimpleAdderIO extends Bundle {
  val inA  = Input(UInt(64.W))
  val inB  = Input(UInt(64.W))
  val inC  = Input(Bool())
  val out  = Output(UInt(64.W))
  val outC = Output(Bool())
}

class SimpleAdder extends Module {
  val io = IO(new SimpleAdderIO())

  val adders = for (i <- 0 to 63) yield {
    val adder = Module(new FullAdder())
    adder
  }

  for (i <- 0 to 62) {
    adders(i + 1).io.inC := adders(i).io.outC;
  }
  adders(0).io.inC := io.inC

  for (i <- 0 to 63) {
    adders(i).io.inA := io.inA(i)
    adders(i).io.inB := io.inB(i)
  }
  io.outC := adders(63).io.outC
  io.out  := VecInit(adders.map(adder => adder.io.out)).asUInt
}

class ALUIO extends Bundle {
  val inA    = Input(UInt(64.W))
  val inB    = Input(UInt(64.W))
  val out    = Output(UInt(64.W))
  val opType = Input(AluMode())
}

class SignalOut extends Bundle {
  val isZero     = Output(Bool())
  val isNegative = Output(Bool())
  val isCarry    = Output(Bool())
}

class ALU extends Module {
  val io       = IO(new ALUIO())
  val signalIO = IO(new SignalOut())

  val simpleAdder = Module(new SimpleAdder())
  // add / sub

  val out = Wire(UInt(64.W))

  simpleAdder.io.inA := io.inA
  simpleAdder.io.inB := Mux(io.opType === AluMode.sub, ~io.inB, io.inB)
  simpleAdder.io.inC := io.opType === AluMode.sub

  val inANotZero = io.inA.orR;
  val inBNotZero = io.inB.orR;

  out := MuxLookup(
    io.opType.asUInt,
    0.U,
    EnumSeq(
      AluMode.add -> simpleAdder.io.out,
      AluMode.and -> (io.inA & io.inB),
      AluMode.sub -> simpleAdder.io.out,
      AluMode.div -> (io.inA.asSInt / io.inB.asSInt).asUInt,
      AluMode.divu -> io.inA / io.inB,
      AluMode.mul -> io.inA * io.inB,
      AluMode.or -> (io.inA | io.inB),
      AluMode.rem -> (io.inA.asSInt % io.inB.asSInt).asUInt,
      AluMode.remu -> io.inA % io.inB,
      AluMode.ll -> (io.inA << io.inB(5, 0)),
      AluMode.ra -> (io.inA.asSInt >> io.inB(5, 0)).asUInt,
      AluMode.rl -> (io.inA >> io.inB(5, 0)),
      AluMode.rlw -> (io.inA(31, 0) >> io.inB(5, 0)),
      AluMode.xor -> (io.inA ^ io.inB)
    )
  )
  io.out              := out
  signalIO.isCarry    := simpleAdder.io.outC
  signalIO.isNegative := out(63)
  signalIO.isZero     := !out.orR
}
