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

  val inst           = RegInit(0x13.U(32.W))
  val predictPC      = RegInit(regIO.pc)
  val lastPC         = RegInit(regIO.pc)
  val needTakeBranch = Wire(Bool())

  val waitAR :: waitR :: waitBranch :: others = Enum(4)
  val fetchFSM = new FSM(
    waitAR,
    List(
      (waitAR, iCacheIO.readReq.fire, waitR),
      // (waitR, iCacheIO.data.fire && needTakeBranch, waitAR),
      (waitR, iCacheIO.data.fire && fromDecode.valid, waitAR),
      (waitR, iCacheIO.data.fire && !fromDecode.valid, waitBranch),
      (waitBranch, fromDecode.valid && needTakeBranch, waitAR),
      (waitBranch, fromDecode.valid && !needTakeBranch, waitAR)
    )
  )

  iCacheIO.data.ready    := fetchFSM.is(waitR)
  iCacheIO.readReq.valid := fetchFSM.is(waitAR)
  iCacheIO.addr          := predictPC

  needTakeBranch := fromDecode.valid && fromDecode.willTakeBranch && fromDecode.branchPc =/= predictPC

  predictPC :=
    Mux(fetchFSM.willChangeTo(waitR), Mux(needTakeBranch, fromDecode.branchPc, predictPC + 4.U), predictPC)
  lastPC := Mux(fetchFSM.willChangeTo(waitR), predictPC, lastPC)

  inst := Mux(iCacheIO.data.fire, iCacheIO.data.bits.asUInt, inst)

  fetchOut.valid := iCacheIO.data.fire

  // fetchout
  fetchOut.bits.debug.pc   := lastPC
  fetchOut.bits.debug.inst := inst
  fetchOut.bits.pc         := lastPC
  fetchOut.bits.inst       := inst

  iCacheIO.debug.pc   := predictPC
  iCacheIO.debug.inst := inst

  iCacheIO.writeReq.valid     := false.B
  iCacheIO.writeReq.bits.data := DontCare
  iCacheIO.writeReq.bits.mask := DontCare
  iCacheIO.writeRes.ready     := false.B
}
