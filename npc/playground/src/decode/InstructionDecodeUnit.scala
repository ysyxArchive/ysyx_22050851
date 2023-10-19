import chisel3._
import chisel3.experimental.BundleLiterals._
import chisel3.util._
import utils._
import decode._
import execute._

class InstructionDecodeUnit extends Module {
  val regIO          = IO(Input(new RegisterFileIO()))
  val iCacheIO       = IO(Flipped(new CacheIO(64, 64)))
  val decodeOut      = IO(Decoupled(new ExeIn()))
  val controlDecoder = Module(new InstContorlDecoder)

  val inst = RegInit(0x13.U(32.W))

  val idle :: waitAR :: waitR :: waitSend :: others = Enum(4)
  val decodeFSM = new FSM(
    waitAR,
    List(
      (waitAR, iCacheIO.readReq.fire, waitR),
      (waitR, iCacheIO.data.fire, waitSend),
      (waitSend, decodeOut.fire, idle),
      (idle, decodeOut.ready, waitAR)
    )
  )

  iCacheIO.data.ready    := decodeFSM.is(waitR)
  iCacheIO.readReq.valid := decodeFSM.is(waitAR)
  iCacheIO.addr          := regIO.pc

  inst := Mux(iCacheIO.data.fire, iCacheIO.data.bits.asUInt, inst)

  // decodeout.control
  controlDecoder.input   := inst
  decodeOut.bits.control := controlDecoder.output

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
  decodeOut.bits.data.imm := MuxLookup(controlDecoder.output.insttype, immI)(
    EnumSeq(
      InstType.I -> immI,
      InstType.S -> immS,
      InstType.U -> immU,
      InstType.B -> immB,
      InstType.J -> immJ
    )
  )
  decodeOut.bits.data.src1 := rs1
  decodeOut.bits.data.src2 := rs2
  decodeOut.bits.data.dst  := rd

  // decodeout.valid
  decodeOut.valid := decodeFSM.is(waitSend)

  iCacheIO.writeReq.valid     := false.B
  iCacheIO.writeReq.bits.data := DontCare
  iCacheIO.writeReq.bits.mask := DontCare
  iCacheIO.writeRes.ready     := false.B
}
