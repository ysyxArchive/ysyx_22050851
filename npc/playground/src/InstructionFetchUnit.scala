import chisel3._
import chisel3.experimental.BundleLiterals._
import chisel3.util._
import utils._
import decode._
import execute._

class InstructionFetchUnit extends Module {
  val regIO      = IO(Input(new RegReadIO()))
  val fetchOut   = IO(Decoupled(new DecodeIn()))
  val iCacheIO   = IO(Flipped(new CacheIO(64, 64)))
  val fromDecode = IO(Flipped(new DecodeBack()))

  val inst      = RegInit(0x13.U(32.W))
  val predictPC = RegInit(regIO.pc)
  // val branched      = RegInit(false.B)
  // val takeBranchReg = Reg(Bool())
  val needTakeBranch = Wire(Bool())
  val branchPC       = Reg(UInt(64.W))

  val waitAR :: waitR :: waitSend :: others = Enum(4)
  val fetchFSM = new FSM(
    waitAR,
    List(
      (waitAR, iCacheIO.readReq.fire, waitR),
      (waitR, iCacheIO.data.fire && fromDecode.willTakeBranch || needTakeBranch, waitAR),
      (waitR, iCacheIO.data.fire, waitSend),
      (waitSend, fetchOut.fire || needTakeBranch, waitAR)
    )
  )

  iCacheIO.data.ready    := fetchFSM.is(waitR)
  iCacheIO.readReq.valid := fetchFSM.is(waitAR)
  iCacheIO.addr          := Mux(fetchFSM.changedFrom(waitR), branchPC, predictPC)

  needTakeBranch := (fromDecode.willTakeBranch && fromDecode.branchPc =/= predictPC) || RegNext(
    !fetchFSM.willChangeTo(waitAR) && fromDecode.willTakeBranch && fromDecode.branchPc =/= predictPC
  )
  branchPC := Mux(fromDecode.willTakeBranch && fromDecode.branchPc =/= predictPC, fromDecode.branchPc, branchPC)

  predictPC :=
    Mux(fetchFSM.willChangeTo(waitAR), Mux(needTakeBranch, branchPC, predictPC + 4.U), predictPC)

  inst := Mux(iCacheIO.data.fire, iCacheIO.data.bits.asUInt, inst)

  fetchOut.valid := fetchFSM.is(waitSend)

  iCacheIO.writeReq.valid     := false.B
  iCacheIO.writeReq.bits.data := DontCare
  iCacheIO.writeReq.bits.mask := DontCare
  iCacheIO.writeRes.ready     := false.B

  // fetchout
  fetchOut.bits.debug.pc   := predictPC
  fetchOut.bits.debug.inst := inst
  fetchOut.bits.pc         := predictPC
  fetchOut.bits.inst       := inst
}
