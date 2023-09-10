import chisel3._
import chisel3.util._
import utils.FSM

class CacheIO(dataWidth: Int, addrWidth: Int) extends Bundle {
  val addr    = Input(UInt(addrWidth.W))
  val readReq = Flipped(Decoupled())
  val data    = Decoupled(UInt(dataWidth.W))
  val writeReq = Flipped(Decoupled(new Bundle {
    val data = UInt(dataWidth.W)
    val mask = UInt(log2Ceil(dataWidth).W)
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
class Cache(cellByte: Int = 64, wayCnt: Int = 4, groupSize: Int = 4, addrWidth: Int = 64, dataWidth: Int = 64)
    extends Module {
  assert(1 << log2Ceil(cellByte) == cellByte)
  assert(1 << log2Ceil(wayCnt) == wayCnt)
  assert(1 << log2Ceil(groupSize) == groupSize)
  val totalByte   = cellByte * groupSize * wayCnt
  val indexOffset = log2Ceil(cellByte)
  val tagOffset   = log2Ceil(cellByte) + log2Ceil(wayCnt)

  val io    = IO(new CacheIO(dataWidth, addrWidth))
  val axiIO = IO(new AxiLiteIO(UInt(dataWidth.W), addrWidth))

  val slotsPerLine = cellByte * 8 / axiIO.dataWidth

  val cacheMem = RegInit(
    VecInit(
      Seq.fill(wayCnt)(VecInit(Seq.fill(groupSize)(0.U.asTypeOf(new CacheLine(addrWidth - tagOffset, cellByte)))))
    )
  )

  val hit     = Wire(Bool())
  val isDirty = Wire(Bool())

  val isRead = Reg(Bool())
  val addr   = Reg(UInt(addrWidth.W))

  val idle :: sendRes :: sendReq :: waitRes :: writeData :: sendWReq :: waitWRes :: others = Enum(10)

  val counter = RegInit(0.U(log2Ceil(slotsPerLine).W))
  counter := PriorityMux(
    Seq(
      (counter === slotsPerLine.U) -> 0.U,
      axiIO.R.fire -> (counter + 1.U),
      true.B -> counter
    )
  )
  val cacheFSM = new FSM(
    idle,
    List(
      (idle, io.readReq.fire && hit, sendRes),
      (idle, io.readReq.fire && !hit && !isDirty, sendReq),
      (idle, io.readReq.fire && !hit && isDirty, sendWReq),
      (idle, io.writeReq.fire && hit, writeData),
      (idle, io.writeReq.fire && !hit && !isDirty, sendReq),
      (idle, io.writeReq.fire && !hit && isDirty, sendWReq),
      (sendRes, io.data.fire, idle),
      (sendReq, axiIO.AR.fire, waitRes),
      (waitRes, axiIO.R.fire && (counter =/= (slotsPerLine - 1).U), sendReq),
      (waitRes, axiIO.R.fire && (counter === (slotsPerLine - 1).U) && isRead, sendRes),
      (waitRes, axiIO.R.fire && (counter === (slotsPerLine - 1).U) && !isRead, writeData),
      (sendWReq, axiIO.AW.fire && axiIO.W.fire, waitWRes),
      (waitWRes, axiIO.B.fire && (counter =/= (slotsPerLine - 1).U), sendWReq),
      (waitWRes, axiIO.B.fire && (counter === (slotsPerLine - 1).U), sendReq),
      (writeData, io.writeRes.fire, idle)
    )
  )

  isRead := Mux(cacheFSM.is(idle), io.readReq.fire, isRead)

  val replaceIndex = RegInit(0.U(log2Ceil(groupSize).W))

  val tag    = Mux(cacheFSM.is(idle), io.addr, addr)(addrWidth - 1, tagOffset)
  val index  = Mux(cacheFSM.is(idle), io.addr, addr)(tagOffset - 1, indexOffset)
  val offset = Mux(cacheFSM.is(idle), io.addr, addr)(indexOffset - 1, 0)

  val wayValid    = cacheMem(index).map(line => line.valid && line.tag === tag)
  val targetIndex = Mux1H(wayValid, Seq.tabulate(groupSize)(index => index.U))
  val data        = cacheMem(index)(targetIndex).data

  hit     := wayValid.reduce(_ || _)
  isDirty := cacheMem(index)(replaceIndex).dirty

  // when idle
  addr              := Mux(io.readReq.fire || io.writeReq.fire, io.addr, addr)
  io.readReq.ready  := cacheFSM.is(idle) && io.readReq.valid
  io.writeReq.ready := cacheFSM.is(idle) && io.writeReq.valid
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
  val mask       = Reverse(Cat(Seq.tabulate(slotsPerLine)(index => Fill(axiIO.dataWidth, UIntToOH(counter)(index)))))
  val maskedData = Fill(slotsPerLine, axiIO.R.bits.data.asUInt) & mask
  for (i <- 0 until wayCnt) {
    when(cacheFSM.is(waitRes) && index === i.U && axiIO.R.fire) {
      cacheMem(i)(replaceIndex).data := maskedData | (cacheMem(i)(replaceIndex).data & ~mask)
      when(counter === (slotsPerLine - 1).U) {
        cacheMem(i)(replaceIndex).tag   := tag
        cacheMem(i)(replaceIndex).valid := true.B
        cacheMem(i)(replaceIndex).dirty := false.B
      }
    }
  }
  axiIO.R.ready := cacheFSM.is(waitRes)
  // when writeData
  val dataWriteReq = Reg(io.writeReq.bits.cloneType)
  dataWriteReq      := Mux(io.writeReq.fire, io.writeReq.bits, dataWriteReq)
  io.writeRes.valid := cacheFSM.is(writeData)

  // ....001111111000...
  val writePositionMask = Reverse(
    Cat(
      Seq.tabulate(slotsPerLine)(index => Fill(dataWidth, UIntToOH(offset)(index)))
    )
  )
  // ...1111110011111...
  val writeMask = ~writePositionMask | (writePositionMask &
    Reverse(
      Cat(
        Seq.tabulate(slotsPerLine)(_ =>
          Cat(Seq.tabulate(dataWriteReq.mask.getWidth)(index => dataWriteReq.mask(index)))
        )
      )
    ))
  val maskedWriteData = Fill(slotsPerLine, dataWriteReq.data) & ~writeMask
  for (i <- 0 until wayCnt) {
    when(cacheFSM.is(writeData) && index === i.U && cacheMem(i)(index).valid) {
      cacheMem(i)(targetIndex).data  := maskedWriteData | (cacheMem(i)(replaceIndex).data & writeMask)
      cacheMem(i)(targetIndex).dirty := true.B
    }
  }
  // when sendWReq
  axiIO.AW.valid     := cacheFSM.is(sendWReq)
  axiIO.AW.bits.addr := Cat(tag, index, counter << log2Ceil(axiIO.dataWidth / 8))
  axiIO.W.valid      := cacheFSM.is(sendWReq)
  axiIO.W.bits.data := PriorityMux(
    Seq.tabulate(slotsPerLine)(index => ((index.U === counter) -> data((index + 1) * dataWidth - 1, index * dataWidth)))
  )
  axiIO.W.bits.strb := Fill(log2Ceil(dataWidth), true.B)
  //when  waitWRes
  axiIO.B.ready := true.B

  axiIO.AW.bits.id   := DontCare
  axiIO.AW.bits.prot := DontCare
  when(cacheFSM.is(idle)) {
    val addr = io.addr
    when(io.writeReq.fire) {
      val data = io.writeReq.bits.data
      printf("cache writing, addr is %x, data is %x\n", addr, data)
    }
    when(io.readReq.fire) {
      printf("cache reading, addr is %x\n", addr)
    }
  }
}
