import chisel3.experimental._
import chisel3._
import chisel3.util._
import decode._
import os.read

class InstructionExecuteUnit extends Module {
  val in       = IO(Flipped(Decoupled(Vec(2, Operation()))))
  val regIO    = IO(Flipped(new RegisterFileIO()))
  val blackBox = Module(new BlackBoxHalt);

  val ops       = RegInit(in.bits)
  val opPointer = RegInit(0.U(1.W))
  val bubbling  = RegInit(false.B)

  val isDoubleOps = ops(1).opType =/= OperationType.nothing.asUInt
  val bubbleNext  = ops(opPointer).dst.stype === SourceType.pc.asUInt && !bubbling;
  val ready       = (!isDoubleOps || isDoubleOps === opPointer) && ~bubbleNext;

  ops       := Mux(in.valid && in.ready, in.bits, ops)
  opPointer := Mux(bubbleNext, opPointer, Mux(opPointer === 0.U, Mux(isDoubleOps, 1.U, 0.U), 0.U));
  bubbling  := ~bubbling && bubbleNext;

  in.ready := ready

  val op = ops(opPointer)

  regIO.waddr  := Mux(bubbling, 0.U, op.dst.value(4, 0))
  regIO.raddr1 := Mux(op.src1.stype === SourceType.reg.asUInt, op.src1.value(4, 0), 0.U(5.W))
  regIO.raddr2 := Mux(op.src2.stype === SourceType.reg.asUInt, op.src2.value(4, 0), 0.U(5.W))

  val src1val =
    MuxLookup(
      op.src1.stype,
      0.U,
      Seq(
        (SourceType.imm.asUInt) -> op.src1.value,
        (SourceType.reg.asUInt) -> regIO.out1,
        (SourceType.npc.asUInt) -> regIO.npc,
        (SourceType.pc.asUInt) -> regIO.pc
      )
    )
  val src2val =
    MuxLookup(
      op.src2.stype,
      0.U,
      Seq(
        (SourceType.imm.asUInt) -> op.src2.value,
        (SourceType.reg.asUInt) -> regIO.out2,
        (SourceType.npc.asUInt) -> regIO.npc,
        (SourceType.pc.asUInt) -> regIO.pc
      )
    )

  val ans = MuxLookup(
    op.opType.asUInt,
    0.U,
    Seq(
      OperationType.add.asUInt -> (src1val + src2val),
      OperationType.move.asUInt -> src1val
    )
  )

  blackBox.io.halt     := op.opType === OperationType.halt.asUInt
  blackBox.io.bad_halt := op.opType === OperationType.noMatch.asUInt;
  regIO.wdata          := ans
  regIO.regWrite       := Mux(bubbling, true.B, op.dst.stype === SourceType.reg.asUInt)
  regIO.pcWrite        := Mux(bubbling, false.B, op.dst.stype === SourceType.pc.asUInt)
}
