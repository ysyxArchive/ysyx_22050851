import chisel3.experimental._
import chisel3._
import chisel3.util._
import decode._
import os.read

class InstructionExecuteUnit extends Module {
  val in       = IO(Flipped(Decoupled(Vec(2, Operation()))))
  val regIO    = IO(Flipped(new RegisterFileIO()))
  val blackBox = Module(new BlackBoxHalt);

  val ops = in.bits;
  val op0 = ops(0);
  val op1 = ops(1);

  in.ready := true.B

  regIO.raddr1 := Mux(
    op1.src1.stype === SourceType.reg.asUInt,
    op1.src1.value(4, 0),
    op1.src2.value(4, 0)
  )
  regIO.raddr0 := Mux(
    op0.src1.stype === SourceType.reg.asUInt,
    op0.src1.value(4, 0),
    op0.src2.value(4, 0)
  );

  val op0src1val =
    MuxLookup(
      op0.src1.stype,
      0.U,
      Seq(
        (SourceType.imm.asUInt) -> op0.src1.value,
        (SourceType.reg.asUInt) -> regIO.out0,
        (SourceType.npc.asUInt) -> regIO.npc,
        (SourceType.pc.asUInt) -> regIO.pc
      )
    );
  val op0src2val =
    MuxLookup(
      op0.src1.stype,
      0.U,
      Seq(
        (SourceType.imm.asUInt) -> op0.src2.value,
        (SourceType.reg.asUInt) -> regIO.out0,
        (SourceType.npc.asUInt) -> regIO.npc,
        (SourceType.pc.asUInt) -> regIO.pc
      )
    );
  val op1src1val =
    MuxLookup(
      op0.src1.stype,
      0.U,
      Seq(
        (SourceType.imm.asUInt) -> op1.src1.value,
        (SourceType.reg.asUInt) -> regIO.out1,
        (SourceType.npc.asUInt) -> regIO.npc,
        (SourceType.pc.asUInt) -> regIO.pc
      )
    );
  val op1src2val =
    MuxLookup(
      op0.src1.stype,
      0.U,
      Seq(
        (SourceType.imm.asUInt) -> op1.src2.value,
        (SourceType.reg.asUInt) -> regIO.out1,
        (SourceType.npc.asUInt) -> regIO.npc,
        (SourceType.pc.asUInt) -> regIO.pc
      )
    );

  val ans0 = MuxLookup(
    op0.opType.asUInt,
    0.U,
    Seq(
      OperationType.add.asUInt -> (op0src1val + op0src2val),
      OperationType.move.asUInt -> op0src1val
    )
  )
  val ans1 = MuxLookup(
    op1.opType.asUInt,
    0.U,
    Seq(
      OperationType.add.asUInt -> (op1src1val + op1src2val),
      OperationType.move.asUInt -> op1src1val
    )
  )

  blackBox.io.halt     := op0.opType === OperationType.halt.asUInt
  blackBox.io.bad_halt := op0.opType === OperationType.noMatch.asUInt;
  regIO.wdata          := Mux(op0.dst.stype === SourceType.reg.asUInt, ans0, ans1)
  regIO.waddr := Mux(
    op0.dst.stype === SourceType.reg.asUInt,
    op0.dst.value,
    Mux(op1.dst.stype === SourceType.reg.asUInt, op1.dst.value, 0.U)
  )

  regIO.dnpc    := Mux(op0.dst.stype === SourceType.npc.asUInt, ans0, ans1)
  regIO.pcWrite := op0.dst.stype === SourceType.npc.asUInt || op1.dst.stype === SourceType.npc.asUInt

}
