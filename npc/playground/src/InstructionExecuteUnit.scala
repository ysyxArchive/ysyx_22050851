import chisel3.experimental._
import chisel3._
import chisel3.util._

class InstructionExecuteUnit extends Module {
  val in = Flipped(Decoupled(Operation()))
  val regIO = Flipped(new RegisterFileIO())

  regIO.raddr1 := Mux(in.bits.src1.isReg, in.bits.src1.value, 0.U)
  regIO.raddr2 := Mux(in.bits.src2.isReg, in.bits.src2.value, 0.U)

  val src1 = Mux(in.bits.src1.isReg, regIO.rdata1, in.bits.src1.value)
  val src2 = Mux(in.bits.src1.isReg, regIO.rdata2, in.bits.src2.value)

  val ans = MuxLookup(in.bits.opType, OperationType.noMatch, Seq(
    OperationType.add -> (src1 + src2)
  ))
  regIO.waddr := regIO.wen
  regIO.wdata := ans
  regIO.wen := RegNext(false.B, true.B)

}
