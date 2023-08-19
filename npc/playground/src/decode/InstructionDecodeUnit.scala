import chisel3._
import chisel3.experimental.ChiselEnum
import chisel3.experimental.BundleLiterals._
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
  val default = new DecodeDataOut().Lit(_.dst -> 0.U, _.src1 -> 0.U, _.src2 -> 0.U, _.imm -> 0.U)
}

class DecodeOut extends Bundle {
  val valid   = Output(Bool())
  val data    = Output(new DecodeDataOut);
  val control = Output(new DecodeControlOut);
}

object DecodeOut {
  val default =
    new DecodeOut().Lit(_.control -> DecodeControlOut.default(), _.data -> DecodeDataOut.default, _.valid -> false.B)
}

class InstructionDecodeUnit extends Module {
  val regIO     = IO(Input(new RegisterFileIO()))
  val instIn    = IO(new FetchDecodeAxiIO())
  val decodeOut = IO(new DecodeOut)

  val controlDecoder = Module(new InstContorlDecoder)

  val inst = instIn.R.bits.data

  instIn.R.ready      := true.B
  instIn.AR.valid     := true.B
  instIn.AR.bits.id   := 0.U
  instIn.AR.bits.prot := 0.U
  instIn.AR.bits.addr := regIO.pc

  controlDecoder.input := inst
  decodeOut.control    := Mux(instIn.R.valid, controlDecoder.output, DecodeControlOut.default())

  val rs1  = inst(19, 15)
  val rs2  = inst(24, 20)
  val rd   = inst(11, 7)
  val immI = Utils.signExtend(inst(31, 20), 12)
  val immS = Utils.signExtend(Cat(inst(31, 25), inst(11, 7)), 12)
  val immU = Utils.signExtend(inst(31, 12), 20) << 12
  val immB = Cat(Utils.signExtend(inst(31), 1), inst(7), inst(30, 25), inst(11, 8), 0.U(1.W))
  val immJ = Utils.signExtend(
    Cat(inst(31), inst(19, 12), inst(20), inst(30, 21), 0.U(1.W)),
    20
  );

  decodeOut.data.imm := MuxLookup(
    controlDecoder.output.insttype, 
    DontCare,
    EnumSeq(
      InstType.I -> immI,
      InstType.S -> immS,
      InstType.U -> immU,
      InstType.B -> immB,
      InstType.J -> immJ
    )
  )
  decodeOut.data.src1 := rs1
  decodeOut.data.src2 := rs2
  decodeOut.data.dst  := rd

  decodeOut.valid := instIn.R.fire

}
