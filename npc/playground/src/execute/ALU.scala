package execute

import chisel3._
import chisel3.util.MuxLookup
import decode.OperationType
import chisel3.util.Cat
import chisel3.util.Reverse

object ALUUtils {
  val width = 2
  // 顺序：isZero, isNegative
  def ALUSignals(isZero: UInt, isNegative: UInt): UInt = VecInit(Seq(isZero, isNegative)).asUInt
  def test(aluSignal: UInt, checker: UInt): Bool = {
    val posChecker = checker(width * 2 - 1, width)
    val negChecker = checker(width - 1, 0)
    ((aluSignal & ~negChecker) & (~aluSignal & ~posChecker)).andR
  }
  val isZero      = "b1000".U
  val notZero     = "b0010".U
  val isNegative  = "b0100".U
  val notNegative = "b0001".U
  val none        = 0.U((width * 2).W)
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
  val inA = Input(UInt(64.W))
  val inB = Input(UInt(64.W))
  val inC = Input(Bool())
  val out = Output(UInt(64.W))
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

  io.out := VecInit(adders.map(adder => adder.io.out)).asUInt
}

class ALUIO extends Bundle {
  val inA     = Input(UInt(64.W))
  val inB     = Input(UInt(64.W))
  val out     = Output(UInt(64.W))
  val signals = Output(UInt(2.W))
  val opType  = Input(ALUType())
}

class ALU extends Module {
  val io = IO(new ALUIO())

  val simpleAdder = Module(new SimpleAdder())
  // add / sub

  val out = Wire(UInt(64.W))

  simpleAdder.io.inA := io.inA
  simpleAdder.io.inB := Mux(io.opType === ALUType.sub, ~io.inB, io.inB)
  simpleAdder.io.inC := io.opType === ALUType.sub

  out := MuxLookup(
    io.opType.asUInt,
    0.U,
    Seq(
      ALUType.add.asUInt -> simpleAdder.io.out,
      ALUType.sub.asUInt -> simpleAdder.io.out
    )
  )
  io.out     := out
  io.signals := ALUUtils.ALUSignals(out.orR, out(63))
}
