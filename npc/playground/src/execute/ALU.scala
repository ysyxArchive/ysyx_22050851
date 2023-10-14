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
    val isImmidiate = Bool()
    val out         = UInt(64.W)
    val signals     = new SignalIO()
  })
}

class ALU extends Module {
  val io = IO(new ALUIO())

  val simpleAdder = Module(new SimpleAdder())
  val multiplier  = Module(new SimpleMultiplier())

  val immOut = Wire(UInt(64.W))

  val mulOps = VecInit(Seq(AluMode.mul, AluMode.mulw).map(t => t.asUInt))

  val inA        = io.in.bits.inA
  val inB        = io.in.bits.inB
  val opType     = io.in.bits.opType
  val inANotZero = inA.orR;
  val inBNotZero = inB.orR;
  val isImm      = !mulOps.contains(io.in.bits.opType.asUInt)

  simpleAdder.io.inA := inA
  simpleAdder.io.inB := Mux(opType === AluMode.sub, ~inB, inB)
  simpleAdder.io.inC := opType === AluMode.sub

  val normal :: busyMul :: others = util.Enum(2)
  val aluFSM = new FSM(
    normal,
    List(
      (normal, io.in.fire && mulOps.contains(io.in.bits.opType.asUInt), busyMul),
      (busyMul, multiplier.io.outValid, normal)
    )
  )

  multiplier.io.multiplicand := io.in.bits.inA
  multiplier.io.multiplier   := io.in.bits.inB
  multiplier.io.flush        := false.B
  multiplier.io.mulValid     := aluFSM.trigger(normal, busyMul)
  multiplier.io.mulSigned    := false.B
  multiplier.io.mulw         := io.in.bits.opType.asUInt === AluMode.mulw.asUInt

  immOut := MuxLookup(opType.asUInt, 0.U)(
    EnumSeq(
      AluMode.add -> simpleAdder.io.out,
      AluMode.and -> (inA & inB),
      AluMode.sub -> simpleAdder.io.out,
      AluMode.div -> (inA.asSInt / inB.asSInt).asUInt,
      AluMode.divu -> inA / inB,
      AluMode.or -> (inA | inB),
      AluMode.rem -> (inA.asSInt % inB.asSInt).asUInt,
      AluMode.remu -> inA % inB,
      AluMode.ll -> (inA << inB(5, 0)),
      AluMode.ra -> (inA.asSInt >> inB(5, 0)).asUInt,
      AluMode.rl -> (inA >> inB(5, 0)),
      AluMode.rlw -> (inA(31, 0) >> inB(5, 0)),
      AluMode.xor -> (inA ^ inB)
    )
  )
  val out = Mux(aluFSM.trigger(busyMul, normal), multiplier.io.resultLow, immOut)
  io.out.valid                   := (aluFSM.is(normal) && io.in.fire && isImm) || aluFSM.trigger(busyMul, normal)
  io.out.bits.isImmidiate        := isImm
  io.out.bits.out                := out
  io.out.bits.signals.isCarry    := simpleAdder.io.outC
  io.out.bits.signals.isNegative := out(63)
  io.out.bits.signals.isZero     := !out.orR

  io.in.ready := aluFSM.is(normal)
}
