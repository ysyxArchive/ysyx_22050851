import chisel3._
import chisel3.experimental.ChiselEnum
import chisel3.experimental.BundleLiterals._
import chisel3.util._
import utils._
import decode._
import execute._
import os.stat

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
  val memAxiM        = MemReadOnlyAxiLiteIO.master()
  val decodeOut      = IO(new DecodeOut)
  val controlDecoder = Module(new InstContorlDecoder)

  val busy :: waitAR :: waitR :: waitSend :: others = Enum(4)

  val decodeFSM = new FSM(
    waitAR,
    List(
      (waitAR, memAxiM.AR.fire, waitR),
      (waitR, memAxiM.R.fire, waitSend),
      (waitSend, true.B, busy),
      (busy, decodeOut.done, waitAR)
    )
  )
  val decodeStatus = decodeFSM.status

  val inst      = RegInit(0x13.U(64.W))
  val instValid = RegInit(false.B)

  memAxiM.R.ready      := decodeStatus === waitR
  memAxiM.AR.valid     := decodeStatus === waitAR
  memAxiM.AR.bits.id   := 0.U
  memAxiM.AR.bits.prot := 0.U
  memAxiM.AR.bits.addr := regIO.pc

  inst      := Mux(memAxiM.R.fire, memAxiM.R.bits.asUInt, inst)
  instValid := Mux(instValid, !decodeOut.done, memAxiM.R.fire)

  controlDecoder.input := inst
  decodeOut.control    := controlDecoder.output

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

  decodeOut.valid := decodeStatus === busy

}
