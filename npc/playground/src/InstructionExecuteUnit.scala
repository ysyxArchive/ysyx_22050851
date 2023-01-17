import chisel3.experimental._
import chisel3._
import chisel3.util._

class InstructionExecuteUnit extends Module {
  val in    = IO(Flipped(Decoupled(Operation())))
  val regIO = IO(Flipped(new RegisterFileIO()))
  val op    = in.bits

  val inReady = RegInit(true.B)
  in.ready := inReady

  val writeEnable = RegInit(true.B)
  val writeAddr   = RegInit(0.U(5.W))
  regIO.wen   := writeEnable
  regIO.waddr := op.dst.value(4,0)

  regIO.raddr1 := Mux(op.src1.isReg, op.src1.value(4, 0), 0.U(5.W))
  regIO.raddr2 := Mux(op.src2.isReg, op.src2.value(4, 0), 0.U(5.W))

  val src1val = Mux(op.src1.isReg, regIO.out1, op.src1.value)
  val src2val = Mux(op.src2.isReg, regIO.out2, op.src2.value)

  val ans = MuxLookup(
    in.bits.opType.asUInt,
    0.U,
    Seq(
      OperationType.add.asUInt -> (src1val + src2val)
    )
  )
  regIO.wdata := ans

}
