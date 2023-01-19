import chisel3.experimental._
import chisel3._
import chisel3.util._

class InstructionExecuteUnit extends Module {
  val in    = IO(Flipped(Decoupled(Operation())))
  val regIO = IO(Flipped(new RegisterFileIO()))
  val op    = in.bits

  val inReady = RegInit(true.B)
  in.ready := inReady

  val blackBox = Module(new BlackBoxAdd);
  blackBox.io.halt := false.B

  regIO.waddr := op.dst.value(4, 0)

  regIO.raddr1 := Mux(op.src1.stype === SourceType.reg.asUInt, op.src1.value(4, 0), 0.U(5.W))
  regIO.raddr2 := Mux(op.src2.stype === SourceType.reg.asUInt, op.src2.value(4, 0), 0.U(5.W))

  val src1val =
    MuxLookup(
      op.src1.stype,
      0.U,
      Seq(
        (SourceType.imm.asUInt) -> op.src1.value,
        (SourceType.reg.asUInt) -> regIO.out1,
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
        (SourceType.pc.asUInt) -> regIO.pc
      )
    )

  val ans = MuxLookup(
    in.bits.opType.asUInt,
    0.U,
    Seq(
      OperationType.add.asUInt -> (src1val + src2val),
      OperationType.move.asUInt -> src1val
    )
  )

  blackBox.io.halt := in.bits.opType === OperationType.halt.asUInt
  regIO.wdata      := ans
  regIO.regWrite   := op.dst.stype === SourceType.reg.asUInt
  regIO.pcWrite    := op.dst.stype === SourceType.pc.asUInt
}
