import chisel3.experimental._
import chisel3._
import chisel3.util._
import decode._
import os.read
import execute._
import utils._

class MemRWIn extends Bundle {
  val debug         = Output(new DebugInfo)
  val data          = Output(new MemDataIn);
  val control       = Output(new ExeControlIn);
  val toDecodeValid = Output(Bool())
}

class MemDataIn extends Bundle {
  val src1     = Output(UInt(5.W))
  val src2     = Output(UInt(5.W))
  val src1Data = Output(UInt(64.W))
  val src2Data = Output(UInt(64.W))
  val dst      = Output(UInt(5.W))
  val imm      = Output(UInt(64.W))
  val alu      = Output(UInt(64.W))
  val signals  = new SignalIO()
  val pc       = Input(UInt(64.W))
  val dnpc     = Input(UInt(64.W))
  val wdata    = Input(UInt(64.W))
}

class MemRWUnit extends Module {
  val memIO    = IO(Flipped(new CacheIO(64, 64)))
  val memIn    = IO(Flipped(Decoupled(new MemRWIn())))
  val memOut   = IO(Decoupled(new WBIn()))
  val toDecode = IO(Flipped(new ForwardData()))
  val fromWbu  = IO(new ForwardData())

  val memInReg = Reg(new MemRWIn())
  memInReg := Mux(memIn.fire, memIn.bits, memInReg)

  val shouldMemWork = memInReg.control.memmode =/= MemMode.no.asUInt
  val memIsRead     = memInReg.control.memmode === MemMode.read.asUInt || memInReg.control.memmode === MemMode.readu.asUInt

  val dataValid = RegInit(false.B)
  dataValid := dataValid ^ memIn.fire ^ memOut.fire

  val regVec = VecInit(Seq(fromWbu).map(bundle => Mux(bundle.dataValid, 0.U, bundle.regIndex)))
  val rs1    = memInReg.data.src1
  val rs2    = memInReg.data.src2
  val src1RawData = MuxCase(
    memInReg.data.src1Data,
    Seq(fromWbu).map(bundle => (bundle.regIndex === rs1 && rs1.orR && bundle.dataValid) -> bundle.data)
  )
  val src2RawData = MuxCase(
    memInReg.data.src2Data,
    Seq(fromWbu).map(bundle => (bundle.regIndex === rs2 && rs2.orR && bundle.dataValid) -> bundle.data)
  )
  val src1Data = Mux(
    memInReg.control.srccast1,
    Utils.cast(src1RawData, 32, 64),
    src1RawData
  )
  val src2Data = Mux(
    memInReg.control.srccast2,
    Utils.cast(src2RawData, 32, 64),
    src2RawData
  )

  val shouldWait = dataValid &&
    ((rs1 =/= 0.U && regVec.contains(rs1)) ||
      (rs2 =/= 0.U && regVec.contains(rs2)))

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

  memIO.readReq.valid      := dataValid && shouldMemWork && !shouldWait && memIsRead
  memIO.addr               := memInReg.data.alu
  memIO.data.ready         := memIsRead
  memIO.writeReq.valid     := dataValid && shouldMemWork && !shouldWait && !memIsRead
  memIO.writeReq.bits.data := src2Data
  memIO.writeReq.bits.mask := memMask
  memIO.debug              := memInReg.debug

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

  memIn.ready := !dataValid || memOut.fire

  memOut.valid              := dataValid && (!shouldMemWork || (memIsRead && memIO.data.fire) || (!memIsRead && memIO.writeReq.fire))
  memOut.bits.debug         := memInReg.debug
  memOut.bits.data.src1     := memInReg.data.src1
  memOut.bits.data.src1Data := src1Data
  memOut.bits.data.dst      := memInReg.data.dst
  memOut.bits.data.pc       := memInReg.data.pc
  memOut.bits.data.dnpc     := memInReg.data.dnpc
  memOut.bits.data.imm      := memInReg.data.imm
  memOut.bits.data.wdata    := Mux(memInReg.toDecodeValid, memInReg.data.wdata, memData)
  memOut.bits.toDecodeValid := toDecode.dataValid

  memOut.bits.control := memInReg.control

  toDecode.regIndex  := Mux(dataValid, memInReg.data.dst, 0.U)
  toDecode.dataValid := dataValid && (memInReg.toDecodeValid || (memInReg.control.regwritemux === RegWriteMux.mem.asUInt && memIO.data.fire))
  toDecode.data      := memOut.bits.data.wdata
  toDecode.csrIndex := Mux(
    dataValid,
    ControlRegisters.behaveDependency(memInReg.control.csrbehave, memInReg.control.csrsetmode, memInReg.data.imm),
    VecInit.fill(3)(0.U(12.W))
  )
}
