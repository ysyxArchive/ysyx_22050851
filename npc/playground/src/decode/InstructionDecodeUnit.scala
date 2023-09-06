import chisel3._
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
  val done    = Input(Bool())
}

object DecodeOut {
  val default =
    new DecodeOut().Lit(_.control -> DecodeControlOut.default(), _.data -> DecodeDataOut.default, _.valid -> false.B)
}

class InstructionDecodeUnit extends Module {
  val regIO          = IO(Input(new RegisterFileIO()))
  val iCacheIO       = IO(Flipped(new CacheIO()))
  val decodeOut      = IO(new DecodeOut)
  val controlDecoder = Module(new InstContorlDecoder)

  val inst = RegInit(0x13.U(64.W))

  val busy :: waitAR :: waitR :: waitSend :: others = Enum(4)
  val decodeFSM = new FSM(
    waitAR,
    List(
      (waitAR, iCacheIO.readReq.fire, waitR),
      (waitR, iCacheIO.data.fire, waitSend),
      (waitSend, true.B, busy),
      (busy, decodeOut.done, waitAR)
    )
  )
  val decodeStatus = decodeFSM.status

  iCacheIO.data.ready    := decodeStatus === waitR
  iCacheIO.readReq.valid := decodeStatus === waitAR
  iCacheIO.readReq.bits  := regIO.pc

  inst := Mux(iCacheIO.data.fire, iCacheIO.data.bits.asUInt, inst)

  // decodeout.control
  controlDecoder.input := inst
  decodeOut.control    := controlDecoder.output

  // decodeout.data
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
  decodeOut.data.imm := MuxLookup(controlDecoder.output.insttype, immI)(
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

  // decodeout.valid
  decodeOut.valid := decodeStatus === waitSend

}
