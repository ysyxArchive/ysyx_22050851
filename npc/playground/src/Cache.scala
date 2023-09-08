import chisel3._
import chisel3.util._
import utils.FSM
import decode.AluMode
import os.group

class CacheIO extends Bundle {
  val readReq = Flipped(Decoupled(UInt(64.W)))
  val data    = Decoupled(UInt(32.W))
  val writeReq = Flipped(Decoupled(new Bundle {
    val addr = UInt(64.W)
    val data = UInt(64.W)
  }))
  val writeRes = Decoupled()
}

class CacheLine(tagWidth: Int, dataByte: Int) extends Bundle {
  val valid = Bool()
  val tag   = UInt(tagWidth.W)
  val dirty = Bool()
  val data  = UInt((dataByte * 8).W)
}

/**
  * @param cellByte 单个cache存储大小
  * @param wayCnt 路数
  * @param groupSize 单路单元数
  * @param addrWidth 地址宽度
  */
class Cache(cellByte: Int = 64, wayCnt: Int = 4, groupSize: Int = 4, addrWidth: Int = 64) extends Module {
  assert(1 << log2Ceil(cellByte) == cellByte)
  assert(1 << log2Ceil(wayCnt) == wayCnt)
  assert(1 << log2Ceil(groupSize) == groupSize)
  val totalByte   = cellByte * groupSize * wayCnt
  val indexOffset = log2Ceil(cellByte)
  val tagOffset   = log2Ceil(cellByte) + log2Ceil(wayCnt)

  val io    = IO(new CacheIO())
  val axiIO = IO(new AxiLiteIO(UInt(64.W), 64))

  // 从axi更新cache需要的请求次数
  val updateTimes = cellByte * 8 / axiIO.dataWidth

  val cacheMem = RegInit(
    VecInit(
      Seq.fill(wayCnt)(VecInit(Seq.fill(groupSize)(0.U.asTypeOf(new CacheLine(addrWidth - tagOffset, cellByte)))))
    )
  )

  val hit     = Wire(Bool())
  val isDirty = Wire(Bool())

  val idle :: sendRes :: sendReq :: waitRes :: writeData :: sendWReq :: waitWRes :: others = Enum(5)

  val counter = RegInit(0.U(log2Ceil(updateTimes).W))
  counter := PriorityMux(
    Seq(
      (counter === updateTimes.U) -> 0.U,
      axiIO.R.fire -> (counter + 1.U),
      true.B -> counter
    )
  )

  val cacheFSM = new FSM(
    idle,
    List(
      (idle, io.readReq.fire && hit, sendRes),
      (idle, io.writeReq.fire, writeData),
      (idle, io.readReq.fire && !hit && !isDirty, sendReq),
      (idle, io.readReq.fire && !hit && isDirty, sendWReq),
      (sendWReq, axiIO.AW.fire && axiIO.W.fire, waitWRes),
      (waitWRes, axiIO.B.fire && (counter =/= (updateTimes - 1).U), sendWReq),
      (waitWRes, axiIO.B.fire && (counter === (updateTimes - 1).U), sendReq),
      (sendReq, axiIO.AR.fire, waitRes),
      (waitRes, axiIO.R.fire && (counter =/= (updateTimes - 1).U), sendReq),
      (waitRes, axiIO.R.fire && (counter === (updateTimes - 1).U), sendRes),
      (sendRes, io.data.fire, idle)
    )
  )

  val replaceIndex = RegInit(0.U(log2Ceil(groupSize).W))

  val tag    = io.readReq.bits(addrWidth - 1, tagOffset)
  val index  = io.readReq.bits(tagOffset - 1, indexOffset)
  val offset = io.readReq.bits(indexOffset - 1, 0)

  val wayValid    = cacheMem(index).map(line => line.valid && line.tag === tag)
  val targetIndex = Mux1H(wayValid, Seq.tabulate(groupSize)(index => index.U))
  val data        = cacheMem(index)(targetIndex).data

  hit     := wayValid.reduce(_ || _)
  isDirty := wayValid(index)(replaceIndex).dirty

  // when idle
  val addr = Reg(UInt(addrWidth.W))
  addr             := Mux(cacheFSM.is(idle), io.readReq.bits, addr)
  io.readReq.ready := cacheFSM.is(idle) && io.readReq.valid
  replaceIndex := Mux(
    cacheFSM.willChangeTo(idle),
    Mux(replaceIndex === (groupSize - 1).U, 0.U, replaceIndex + 1.U),
    replaceIndex
  )
  // when sendRes
  val s = Seq.tabulate(cellByte)(o => ((o.U === offset) -> data(data.getWidth - 1, o * 8)))
  io.data.bits  := PriorityMux(s)
  io.data.valid := cacheFSM.is(sendRes)
  // when sendReq
  axiIO.AR.bits.addr := Cat(Seq(tag, index, counter << log2Ceil(axiIO.dataWidth / 8)))
  axiIO.AR.bits.id   := 0.U
  axiIO.AR.bits.prot := 0.U
  axiIO.AR.valid     := cacheFSM.is(sendReq)
  // when waitRes
  val mask       = Reverse(Cat(Seq.tabulate(updateTimes)(index => Fill(axiIO.dataWidth, UIntToOH(counter)(index)))))
  val maskedData = Fill(updateTimes, axiIO.R.bits.data.asUInt) & mask
  for (i <- 0 until wayCnt) {
    when(cacheFSM.is(waitRes) && index === i.U && axiIO.R.fire) {
      cacheMem(i)(replaceIndex).data := maskedData | (cacheMem(i)(replaceIndex).data & ~mask)
      when(counter === (updateTimes - 1).U) {
        cacheMem(i)(replaceIndex).tag   := tag
        cacheMem(i)(replaceIndex).valid := true.B
      }
    }
  }
  axiIO.R.ready := cacheFSM.is(waitRes)

  // when sendWReq

  axiIO.AW.valid     := false.B
  axiIO.W.valid      := false.B
  axiIO.B.ready      := false.B
  axiIO.AW.bits.id   := DontCare
  axiIO.AW.bits.addr := DontCare
  axiIO.AW.bits.prot := DontCare
  axiIO.W.bits.data  := DontCare
  axiIO.W.bits.strb  := DontCare
}
