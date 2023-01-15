import chisel3.experimental._
import chisel3._
import chisel3.util._

class InstructionExecuteUnit extends Module {
  val in        = Flipped(Decoupled(Operation()))
  val regIO     = Flipped(new RegisterFileIO())
  val operation = RegInit(Operation, in.bits)
  val src1      = operation.src1
  val src2      = operation.src2

  regIO.raddr1 := Mux(src1.isReg === true.B, src1.value, 0.U(64.W))
  regIO.raddr2 := Mux(src2.isReg === true.B, in.bits.src2.value, 0.U(64.W))

  val src1val = Mux(in.bits.src1.isReg, regIO.rdata1, in.bits.src1.value)
  val src2val = Mux(in.bits.src1.isReg, regIO.rdata2, in.bits.src2.value)

  val ans = MuxLookup(
    in.bits.opType.asUInt,
    0.U,
    Seq(
      OperationType.add.asUInt -> (src1val + src2val)
    )
  )
  regIO.waddr := regIO.wen
  regIO.wdata := ans
  regIO.wen   := RegNext(false.B, true.B)

}
