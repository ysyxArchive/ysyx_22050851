import chisel3.experimental._
import chisel3._
import chisel3.util._
import decode._
import os.read
import execute._
import utils._

class MemRWIn extends Bundle {
  val debug   = Output(new DebugInfo)
  val data    = Output(new MemDataIn);
  val control = Output(new ExeControlIn);
}

class MemDataIn extends Bundle {
  val src1     = Output(UInt(5.W))
  val src2     = Output(UInt(5.W))
  val src1Data = Output(UInt(64.W))

  val dst     = Output(UInt(5.W))
  val imm     = Output(UInt(64.W))
  val alu     = Output(UInt(64.W))
  val signals = new SignalIO()
  val pc      = Input(UInt(64.W))
}

class MemRWUnit extends Module {
  val memIO  = IO(Flipped(new CacheIO(64, 64)))
  val memIn  = IO(Flipped(Decoupled(new MemRWIn())))
  val memOut = IO(Decoupled(new WBIn()))

  val memInReg = Reg(new MemRWIn())

  val shouldMemWork = memIn.bits.control.memmode =/= MemMode.no.asUInt
  val memIsRead     = memInReg.control.memmode === MemMode.read.asUInt || memInReg.control.memmode === MemMode.readu.asUInt

  val idle :: waitIn :: waitMemReq :: waitMemRes :: waitOut :: other = Enum(10)

  val memFSM = new FSM(
    idle,
    List(
      (idle, memOut.ready, waitIn),
      (waitIn, memIn.fire && shouldMemWork, waitMemReq),
      (waitIn, memIn.fire && !shouldMemWork, waitOut),
      (waitMemReq, Mux(memIsRead, memIO.readReq.fire, memIO.writeReq.fire), waitMemRes),
      (waitMemRes, Mux(memIsRead, memIO.data.fire, memIO.writeRes.fire), waitOut),
      (waitOut, memOut.fire, idle)
    )
  )

  memInReg := Mux(memIn.fire, memIn.bits, memInReg)
  // mem
  val memlen = MuxLookup(memInReg.control.memlen, 1.U)(
    EnumSeq(
      MemLen.one -> 1.U,
      MemLen.two -> 2.U,
      MemLen.four -> 4.U,
      MemLen.eight -> 8.U
    )
  )

  val memMask = Cat(
    Fill(4, Mux(memlen > 4.U, 1.U, 0.U)),
    Fill(2, Mux(memlen > 2.U, 1.U, 0.U)),
    Fill(1, Mux(memlen > 1.U, 1.U, 0.U)),
    1.U(1.W)
  )
  val memAddrReg = Reg(UInt(64.W))
  memAddrReg := Mux(memFSM.willChangeTo(waitMemReq), memInReg.data.alu, memAddrReg)

  memIO.readReq.valid      := memFSM.is(waitMemReq) && memIsRead && shouldMemWork
  memIO.addr               := memAddrReg
  memIO.data.ready         := memFSM.is(waitMemRes) && memIsRead
  memIO.writeReq.valid     := memFSM.is(waitMemReq) && !memIsRead && shouldMemWork
  memIO.writeReq.bits.data := memInReg.data.src2
  memIO.writeReq.bits.mask := memMask
  memIO.writeRes.ready     := memFSM.is(waitMemRes)
  val memOutRaw = MuxLookup(memInReg.control.memlen, memIO.data.bits)(
    EnumSeq(
      MemLen.one -> memIO.data.asUInt(7, 0),
      MemLen.two -> memIO.data.asUInt(15, 0),
      MemLen.four -> memIO.data.asUInt(31, 0),
      MemLen.eight -> memIO.data.asUInt
    )
  )
  val memData = Mux(
    memInReg.control.memmode === MemMode.read.asUInt,
    Utils.signExtend(memOutRaw, memlen << 3),
    Utils.zeroExtend(memOutRaw, memlen << 3)
  )

  memIn.ready := memFSM.is(waitIn)

  memOut.valid              := memFSM.is(waitOut)
  memOut.bits.debug         := memInReg.debug
  memOut.bits.data.src1     := memInReg.data.src1
  memOut.bits.data.src2     := memInReg.data.src2
  memOut.bits.data.src1Data := memInReg.data.src1Data
  memOut.bits.data.dst      := memInReg.data.dst
  memOut.bits.data.mem      := memData
  memOut.bits.data.alu      := memInReg.data.alu
  memOut.bits.data.mem      := memData
  memOut.bits.data.signals  := memInReg.data.signals
  memOut.bits.data.pc       := memInReg.data.pc
  memOut.bits.data.imm      := memInReg.data.imm
  memOut.bits.control       := memInReg.control
}
