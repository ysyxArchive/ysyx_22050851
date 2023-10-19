import chisel3._
import chisel3.experimental.BundleLiterals._
import chisel3.util._
import utils._
import decode._
import execute._

class DecodeIn extends Bundle {
  val debug = Output(new DebugInfo)
  val pc    = Output(UInt(64.W))
  val inst  = Output(UInt(32.W))
}

class InstructionDecodeUnit extends Module {
  val regIO          = IO(Input(new RegReadIO()))
  val decodeIn       = IO(Flipped(Decoupled(new DecodeIn())))
  val decodeOut      = IO(Decoupled(new ExeIn()))
  val controlDecoder = Module(new InstContorlDecoder)

  val decodeInReg = Reg(new DecodeIn())

  val idle :: waitFetch :: waitSend :: others = Enum(4)
  val decodeFSM = new FSM(
    waitFetch,
    List(
      (waitSend, decodeOut.fire, idle),
      (waitFetch, decodeIn.fire, waitSend),
      (idle, decodeOut.ready, waitFetch)
    )
  )

  decodeInReg := Mux(decodeIn.fire, decodeIn.bits, decodeInReg)

  // decodeout.control
  controlDecoder.input := decodeInReg.inst

  // decodeout.data
  val inst = decodeInReg.inst
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
  // debug
  decodeOut.bits.debug.pc   := regIO.pc
  decodeOut.bits.debug.inst := inst

  decodeIn.ready := decodeFSM.is(waitFetch)

  decodeOut.valid         := decodeFSM.is(waitSend)
  decodeOut.bits.data.pc  := decodeInReg.pc
  decodeout.bits.data.imm := imm
  decodeOut.bits.control  := controlDecoder.output

}
