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

  val inst      = RegInit(0x13.U(32.W))
  val predictPC = RegInit(regIO.pc)
  val lastPC    = RegInit(regIO.pc)
  val dataValid = RegInit(false.B)

  iCacheIO.readReq.valid := !dataValid || fetchOut.fire
  iCacheIO.addr          := predictPC

  val needTakeBranch = fromDecode.valid && fromDecode.willTakeBranch && fromDecode.branchPc =/= lastPC

  dataValid := (dataValid && !fetchOut.fire && !needTakeBranch) || (!needTakeBranch && iCacheIO.readReq.fire && !(dataValid ^ fetchOut.valid))

  predictPC := Mux(needTakeBranch, fromDecode.branchPc, Mux(iCacheIO.readReq.fire, predictPC + 4.U, predictPC))
  lastPC    := Mux(needTakeBranch, fromDecode.branchPc, Mux(iCacheIO.readReq.fire, predictPC, lastPC))

  inst := Mux(iCacheIO.readReq.fire, iCacheIO.readReq.bits.data, inst)

  fetchOut.valid := dataValid && !needTakeBranch

  // fetchout
  fetchOut.bits.debug.pc   := lastPC
  fetchOut.bits.debug.inst := inst
  fetchOut.bits.pc         := lastPC
  fetchOut.bits.inst       := inst
  fetchOut.bits.snpc       := lastPC + 4.U

  iCacheIO.debug.pc   := predictPC
  iCacheIO.debug.inst := inst

  iCacheIO.writeReq.valid     := false.B
  iCacheIO.writeReq.bits.data := DontCare
  iCacheIO.writeReq.bits.mask := DontCare
}
