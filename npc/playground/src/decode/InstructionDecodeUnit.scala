import chisel3._
import chisel3.experimental.ChiselEnum
import chisel3.util._
import utils._
import decode._
import execute._

class DecodeDataOut extends Bundle {
  val src1 = Output(UInt(5.W))
  val src2 = Output(UInt(5.W))
  val dst  = Output(UInt(5.W))
  val imm  = Output(UInt(64.W))
}

object DecodeDataOut {
  def default() = {
    val defaultout = new DecodeDataOut().Lit(_.dst -> 0.U, _.src1 -> 0.U, _.src2 -> 0.U, _.imm -> 0.U)
    // defaultout.dst  := 0.U
    // defaultout.src1 := 0.U
    // defaultout.src2 := 0.U
    // defaultout.imm  := 0.U
    defaultout
  }
}

class DecodeOut extends Bundle {
  val data    = Output(new DecodeDataOut);
  val control = Output(new DecodeControlOut);
}

object DecodeOut {
  def default() = {
    val defaultout = Wire(new DecodeOut)
    defaultout.control := DecodeControlOut.default()
    defaultout.data    := DecodeDataOut.default()
    defaultout
  }
}

class InstructionDecodeUnit extends Module {
  val io = IO(new Bundle {
    val enable = Input(Bool())
    val inst   = Input(UInt(32.W))
    val ready  = Output(Bool())
  })
  val output    = IO(Decoupled(new DecodeOut))
  val decodeOut = Wire(new DecodeOut)

  val controlDecoder = Module(new InstContorlDecoder)

  io.ready := true.B

  controlDecoder.input := io.inst
  decodeOut.control    := controlDecoder.output

  val rs1  = io.inst(19, 15)
  val rs2  = io.inst(24, 20)
  val rd   = io.inst(11, 7)
  val immI = Utils.signExtend(io.inst(31, 20), 12)
  val immS = Utils.signExtend(Cat(io.inst(31, 25), io.inst(11, 7)), 12)
  val immU = Utils.signExtend(io.inst(31, 12), 20) << 12
  val immB = Cat(Utils.signExtend(io.inst(31), 1), io.inst(7), io.inst(30, 25), io.inst(11, 8), 0.U(1.W))
  val immJ = Utils.signExtend(
    Cat(io.inst(31), io.inst(19, 12), io.inst(20), io.inst(30, 21), 0.U(1.W)),
    20
  );

  decodeOut.data.imm := MuxLookup(
    controlDecoder.output.insttype,
    DontCare,
    Seq(
      InstType.I.asUInt -> immI,
      InstType.S.asUInt -> immS,
      InstType.U.asUInt -> immU,
      InstType.B.asUInt -> immB,
      InstType.J.asUInt -> immJ
    )
  )
  decodeOut.data.src1 := rs1
  decodeOut.data.src2 := rs2
  decodeOut.data.dst  := rd

  when(output.ready) {
    output.enq(decodeOut)
  }.otherwise {
    output.bits  := DontCare
    output.valid := false.B
  }
}
