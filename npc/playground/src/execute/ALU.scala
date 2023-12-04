package execute

import chisel3._
import chisel3.util.MuxLookup
import chisel3.util.Cat
import chisel3.util.Reverse
import chisel3.util.Fill
import decode.AluMode
import utils.EnumSeq
import chisel3.util.Decoupled
import utils.FSM
import decode.RegWriteMux
import decode.AluMux2
import chisel3.util.MuxCase

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
class AdderIO extends Bundle {
  val inA  = Input(UInt(64.W))
  val inB  = Input(UInt(64.W))
  val inC  = Input(Bool())
  val out  = Output(UInt(64.W))
  val outC = Output(Bool())
}

class SimpleAdder extends Module {
  val io = IO(new AdderIO())

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

class FastAdder extends Module {
  val io     = IO(new AdderIO())
  val result = io.inA +& io.inB + io.inC
  io.out  := result
  io.outC := result(64)
}

class SignalIO extends Bundle {
  val isZero     = Output(Bool())
  val isNegative = Output(Bool())
  val isCarry    = Output(Bool())
}

class ALUIO extends Bundle {
  val in = Flipped(Decoupled(new Bundle {
    val inA    = UInt(64.W)
    val inB    = UInt(64.W)
    val opType = AluMode()
  }))
  val out = Decoupled(new Bundle {
    val out     = UInt(64.W)
    val signals = new SignalIO()
  })
}

class ALU extends Module {
  val io = IO(new ALUIO())

  // val adder = Module(new SimpleAdder())
  val adder = Module(new FastAdder())
<<<<<<< HEAD
  val multiplier = Module(new BoothMultiplier())
  // val multiplier = Module(new BHMultiplier())
=======
  // val multiplier = Module(new BoothMultiplier())
  val multiplier = Module(new BHMultiplier())
>>>>>>> adaab1e8590675071c22ec50f610816123747f3a
  // val divider    = Module(new SimpleDivider())
  val divider = Module(new R2Divider())

  val mulLOps = VecInit(Seq(AluMode.mul, AluMode.mulw).map(t => t.asUInt))
  val mulHOps = VecInit(Seq(AluMode.mulh, AluMode.mulhsu, AluMode.mulhu).map(t => t.asUInt))
  val mulOps  = VecInit(mulLOps ++ mulHOps)
  val divOps  = VecInit(Seq(AluMode.div, AluMode.divu, AluMode.divw, AluMode.divuw).map(t => t.asUInt))
  val remOps  = VecInit(Seq(AluMode.rem, AluMode.remu, AluMode.remw, AluMode.remuw).map(t => t.asUInt))
  val ops     = VecInit(mulOps ++ divOps ++ remOps)

  val inA          = io.in.bits.inA
  val inB          = io.in.bits.inB
  val opType       = io.in.bits.opType
  val inANotZero   = inA.orR;
  val inBNotZero   = inB.orR;
  val isImm        = !ops.contains(io.in.bits.opType.asUInt)
  val isRem        = Reg(Bool())
  val shouldMulReg = Reg(Bool())
  val shouldDivReg = Reg(Bool())

  val inAReg = Reg(UInt(64.W))
  val inBReg = Reg(UInt(64.W))

  val dataValid = RegInit(false.B)
  dataValid := dataValid ^ io.in.fire ^ io.out.fire

  inAReg := Mux(dataValid, inAReg, io.in.bits.inA)
  inBReg := Mux(dataValid, inBReg, io.in.bits.inB)

  adder.io.inA := inA
  adder.io.inB := Mux(opType === AluMode.sub, ~inB, inB)
  adder.io.inC := opType === AluMode.sub

  val shouldMul = mulOps.contains(io.in.bits.opType.asUInt)
  val shouldDiv = VecInit(divOps ++ remOps).contains(io.in.bits.opType.asUInt)
  shouldMulReg := Mux(io.in.fire, shouldMul, shouldMulReg)
  shouldDivReg := Mux(io.in.fire, shouldDiv, shouldDivReg)
  isRem        := Mux(io.in.fire, remOps.contains(io.in.bits.opType.asUInt), isRem)

  multiplier.io.multiplicand := Mux(dataValid, inAReg, io.in.bits.inA)
  multiplier.io.multiplier   := Mux(dataValid, inBReg, io.in.bits.inB)
  multiplier.io.flush        := false.B
  multiplier.io.mulValid     := io.in.fire && shouldMul
  multiplier.io.mulSigned := MuxLookup(io.in.bits.opType.asUInt, 0.U)(
    EnumSeq(
      AluMode.mulhsu -> 2.U,
      AluMode.mulh -> 3.U
    )
  )
  multiplier.io.mulw   := io.in.bits.opType.asUInt === AluMode.mulw.asUInt
  divider.io.dividend  := Mux(dataValid, inAReg, io.in.bits.inA)
  divider.io.divisor   := Mux(dataValid, inBReg, io.in.bits.inB)
  divider.io.flush     := false.B
  divider.io.divValid  := io.in.fire && shouldDiv
  divider.io.divSigned := VecInit(Seq(AluMode.remw, AluMode.divw).map(t => t.asUInt)).contains(io.in.bits.opType.asUInt)
  divider.io.divw := VecInit(Seq(AluMode.remw, AluMode.remuw, AluMode.divw, AluMode.divuw).map(t => t.asUInt))
    .contains(io.in.bits.opType.asUInt)

  val out = MuxLookup(opType.asUInt, 0.U)(
    EnumSeq(
      AluMode.add -> adder.io.out,
      AluMode.and -> (inA & inB),
      AluMode.sub -> adder.io.out,
      AluMode.or -> (inA | inB),
      AluMode.ll -> (inA << inB(5, 0)),
      AluMode.ra -> (inA.asSInt >> inB(5, 0)).asUInt,
      AluMode.rl -> (inA >> inB(5, 0)),
      AluMode.rlw -> (inA(31, 0) >> inB(4, 0)),
      AluMode.xor -> (inA ^ inB)
    ) ++ mulHOps.map(op => op -> multiplier.io.resultHigh)
      ++ mulLOps.map(op => op -> multiplier.io.resultLow)
      ++ divOps.map(op => op -> divider.io.quotient)
      ++ remOps.map(op => op -> divider.io.remainder)
  )

  io.out.valid                   := (io.in.fire && isImm) || (shouldDivReg && divider.io.outValid) || (shouldMulReg && multiplier.io.outValid)
  io.out.bits.out                := out
  io.out.bits.signals.isCarry    := adder.io.outC
  io.out.bits.signals.isNegative := out(63)
  io.out.bits.signals.isZero     := !out.orR

  io.in.ready := !dataValid
}
