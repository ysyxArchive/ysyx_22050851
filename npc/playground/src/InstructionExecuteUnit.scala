import chisel3.experimental._
import chisel3._
import chisel3.util._

class InstructionExecuteUnit extends Module {
  val in    = IO(Flipped(Decoupled(Operation())))
  val regIO = IO(Flipped(new RegisterFileIO()))

  val inReady   = RegInit(true.B)
  val readyNext = IO(Bool())
  inReady  := Mux(inReady, readyNext, true.B)
  in.ready := inReady

  val writeEnable = RegInit(false.B)
  val writeNext   = IO(Bool())
  val writeAddr   = RegInit(0.U(5.W))
  val writeData   = RegInit(0.U(64.W))
  regIO.wen   := writeEnable
  regIO.waddr := writeAddr
  regIO.wdata := writeData
  writeEnable := Mux(writeEnable, false.B, writeNext)

  val op = in.bits
  regIO.raddr1 := Mux(op.src1.isReg, op.src1.value, 0.U(64.W))
  regIO.raddr2 := Mux(op.src1.isReg, op.src2.value, 0.U(64.W))

  val src1val = Mux(op.src1.isReg, regIO.out1, op.src1.value)
  val src2val = Mux(op.src1.isReg, regIO.out2, op.src2.value)

  val ans = MuxLookup(
    in.bits.opType.asUInt,
    0.U,
    Seq(
      OperationType.add.asUInt -> (src1val + src2val)
    )
  )
  when(in.valid) {
    inReady   := false.B
    writeAddr := regIO.wen
    writeData := ans
    // writeNext := true.B
  }

}
