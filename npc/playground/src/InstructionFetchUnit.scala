import chisel3._
import chisel3.experimental.BundleLiterals._
import chisel3.util._
import utils._
import decode._
import execute._
import chisel3.experimental.prefix

class InstructionFetchUnit extends Module {
  val regIO      = IO(Input(new RegReadIO()))
  val fetchOut   = IO(Decoupled(new DecodeIn()))
  val iCacheIO   = IO(Flipped(new CacheIO(64, 64)))
  val fromDecode = IO(Flipped(new DecodeBack()))

  val inst           = RegInit(0x13.U(32.W))
  val predictPC      = RegInit(regIO.pc)
  val lastPC         = RegInit(regIO.pc)
  val needTakeBranch = Wire(Bool())
  val dataValid      = RegInit(false.B)

  val waitAR :: waitR :: waitBranch :: others = Enum(4)
  val fetchFSM = new FSM(
    waitAR,
    List(
      (waitAR, iCacheIO.readReq.fire && !iCacheIO.data.fire, waitR),
      (waitR, iCacheIO.data.fire, waitAR)
      // (waitR, iCacheIO.data.fire && needTakeBranch, waitAR),
      // (waitR, iCacheIO.data.fire && fromDecode.valid, waitAR),
      // (waitR, iCacheIO.data.fire && !fromDecode.valid, waitBranch),
      // (waitBranch, fromDecode.valid && needTakeBranch, waitAR),
      // (waitBranch, fromDecode.valid && !needTakeBranch, waitAR)
    )
  )

  iCacheIO.data.ready    := !dataValid || fetchOut.fire
  iCacheIO.readReq.valid := fetchFSM.is(waitAR) && fromDecode.valid && !needTakeBranch
  iCacheIO.addr          := predictPC

  dataValid := dataValid ^ iCacheIO.data.fire ^ fetchOut.fire

  // needTakeBranch := fromDecode.valid && fromDecode.willTakeBranch && fromDecode.branchPc =/= predictPC
  needTakeBranch := (!RegNext(fromDecode.willTakeBranch) || RegNext(
    fetchFSM.status
  ) === waitR) && fromDecode.willTakeBranch && fetchFSM.is(waitAR)

  predictPC := Mux(
    needTakeBranch && fetchFSM.is(waitAR),
    fromDecode.branchPc,
    Mux(fetchOut.fire, predictPC + 4.U, predictPC)
  )
  lastPC := Mux((needTakeBranch && fetchFSM.is(waitAR)) || fetchOut.fire, predictPC, lastPC)

  inst := Mux(iCacheIO.data.fire, iCacheIO.data.bits, inst)

  fetchOut.valid := dataValid

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
