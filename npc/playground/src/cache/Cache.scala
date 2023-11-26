import chisel3._
import chisel3.util._
import utils.FSM
import utils.Utils
import utils.DebugInfo

class CacheIO(dataWidth: Int, addrWidth: Int) extends Bundle {
  val addr    = Input(UInt(addrWidth.W))
  val readReq = Flipped(Decoupled())
  val data    = Decoupled(UInt(dataWidth.W))
  val writeReq = Flipped(Decoupled(new Bundle {
    val data = UInt(dataWidth.W)
    val mask = UInt((dataWidth / 8).W)
  }))
  val debug = Input(new DebugInfo())
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
class Cache(
  cellByte:  Int    = 32,
  wayCnt:    Int    = 4,
  groupSize: Int    = 8,
  addrWidth: Int    = 64,
  dataWidth: Int    = 64,
  name:      String = "cache")
    extends Module {
  val totalByte   = cellByte * groupSize * wayCnt
  val indexOffset = log2Ceil(cellByte)
  val tagOffset   = log2Ceil(cellByte) + log2Ceil(wayCnt)

  val blackBoxCache = Module(new BlackBoxCache(wayCnt, groupSize))

  val io          = IO(new CacheIO(dataWidth, addrWidth))
  val axiIO       = IO(new BurstLiteIO(UInt(dataWidth.W), addrWidth))
  val enableDebug = IO(Input(Bool()))

  val replaceIndeices = Wire(Vec(wayCnt, UInt(log2Ceil(groupSize).W)))

  // val cachePolicy = Seq.tabulate(wayCnt)(_ => Module(new NaiveCachePolicy(dataWidth, groupSize)))
  val cachePolicy = Seq.tabulate(wayCnt)(_ => Module(new PLRUCachePolicy(dataWidth, groupSize)))

  val slotsPerLine = cellByte * 8 / dataWidth

  val cacheMem = RegInit(
    VecInit(
      Seq.fill(wayCnt)(VecInit(Seq.fill(groupSize)(0.U.asTypeOf(new CacheLine(addrWidth - tagOffset, cellByte)))))
    )
  )

  val hit     = Wire(Bool())
  val isDirty = Wire(Bool())

  val isRead = Reg(Bool())
  val addr   = Reg(UInt(addrWidth.W))

  val idle :: sendReq :: waitRes :: writeData :: sendWReq :: sendWData :: waitWRes :: directWReq :: directWData :: directWRes :: directRReq :: directRRes :: others =
    Enum(16)

  val counter       = RegInit(0.U(log2Ceil(slotsPerLine).W))
  val shoudDirectRW = io.addr > 0xa0000000L.U
  val cacheFSM = new FSM(
    idle,
    List(
      (idle, io.readReq.fire && shoudDirectRW && !axiIO.AR.fire, directRReq),
      (idle, io.readReq.fire && shoudDirectRW && axiIO.AR.fire, directRRes),
      (idle, io.readReq.fire && !shoudDirectRW && !hit && !isDirty && !axiIO.AR.fire, sendReq),
      (idle, io.readReq.fire && !shoudDirectRW && !hit && !isDirty && axiIO.AR.fire, waitRes),
      (idle, io.readReq.fire && !shoudDirectRW && !hit && isDirty && !axiIO.AW.fire, sendWReq),
      (idle, io.readReq.fire && !shoudDirectRW && !hit && isDirty && axiIO.AW.fire, sendWData),
      (idle, io.writeReq.fire && shoudDirectRW && !axiIO.AW.fire, directWReq),
      (idle, io.writeReq.fire && shoudDirectRW && axiIO.AW.fire, directWData),
      (idle, io.writeReq.fire && !shoudDirectRW && hit, writeData),
      (idle, io.writeReq.fire && !shoudDirectRW && !hit && !isDirty && !axiIO.AR.fire, sendReq),
      (idle, io.writeReq.fire && !shoudDirectRW && !hit && !isDirty && axiIO.AR.fire, waitRes),
      (idle, io.writeReq.fire && !shoudDirectRW && !hit && isDirty && !axiIO.AW.fire, sendWReq),
      (idle, io.writeReq.fire && !shoudDirectRW && !hit && isDirty && axiIO.AW.fire, sendWData),
      (sendReq, axiIO.AR.fire, waitRes),
      (waitRes, axiIO.R.fire && (counter === (slotsPerLine - 1).U) && isRead, idle),
      (waitRes, axiIO.R.fire && (counter === (slotsPerLine - 1).U) && !isRead, writeData),
      (sendWReq, axiIO.AW.fire, sendWData),
      (sendWData, axiIO.W.fire && (counter === (slotsPerLine - 1).U), waitWRes),
      (waitWRes, axiIO.B.fire, sendReq),
      (writeData, true.B, idle),
      (directWReq, axiIO.AW.fire, directWData),
      (directWData, axiIO.W.fire, directWRes),
      (directWRes, axiIO.B.fire, idle),
      (directRReq, axiIO.AR.fire, directRRes),
      (directRRes, axiIO.R.fire && io.data.fire, idle)
    )
  )
  counter := PriorityMux(
    Seq(
      (counter === slotsPerLine.U) -> 0.U,
      (cacheFSM.is(waitRes) && axiIO.R.fire) -> (counter + 1.U),
      (cacheFSM.is(sendWData) && axiIO.W.fire) -> (counter + 1.U),
      true.B -> counter
    )
  )

  isRead := Mux(cacheFSM.is(idle), io.readReq.fire, isRead)

  val tag      = Mux(cacheFSM.is(idle), io.addr, addr)(addrWidth - 1, tagOffset)
  val index    = Mux(cacheFSM.is(idle), io.addr, addr)(tagOffset - 1, indexOffset)
  val offset   = Mux(cacheFSM.is(idle), io.addr, addr)(indexOffset - 1, 0)
  val ioTag    = io.addr(addrWidth - 1, tagOffset)
  val ioIndex  = io.addr(tagOffset - 1, indexOffset)
  val ioOffset = io.addr(indexOffset - 1, 0)

  val offsetReg = Reg(UInt(indexOffset.W))
  offsetReg := Mux(cacheFSM.is(idle), offset, offsetReg)

  val wayValid     = cacheMem(index).map(line => line.valid && line.tag === tag)
  val replaceIndex = replaceIndeices(index)
  val targetIndex  = MuxCase(replaceIndex, Seq.tabulate(groupSize)(index => (wayValid(index) -> index.U)))
  val data         = cacheMem(index)(targetIndex).data

  hit     := wayValid.reduce(_ || _)
  isDirty := cacheMem(index)(replaceIndex).dirty

  for (i <- 0 until wayCnt) {
    replaceIndeices(i) := cachePolicy(i).io.replaceIndex
    cachePolicy(i).io.update := index === i.U && (((io.readReq.fire || io.writeReq.fire) && hit) || cacheFSM
      .willChangeTo(idle))
    cachePolicy(i).io.hit      := hit
    cachePolicy(i).io.hitIndex := targetIndex
  }

  // when idle
  addr              := Mux(io.readReq.fire || io.writeReq.fire, io.addr, addr)
  io.readReq.ready  := cacheFSM.is(idle) && io.readReq.valid
  io.writeReq.ready := cacheFSM.is(idle) && io.writeReq.valid && !io.readReq.valid

  // when sendRes or directRBack
  val s = Seq.tabulate(cellByte)(o => ((o.U === ioOffset) -> data(data.getWidth - 1, o * 8)))
  io.data.bits := MuxCase(
    0.U,
    Seq(
      cacheFSM.is(directRRes) -> axiIO.R.bits.data,
      cacheFSM.is(idle) -> PriorityMux(s),
      cacheFSM.is(waitRes) -> PriorityMux(s)
    )
  )
  io.data.valid := (cacheFSM.is(directRRes) && axiIO.R.valid) ||
    (cacheFSM.is(idle) && io.readReq.fire && hit) ||
    (cacheFSM.is(waitRes) && isRead && tag === ioTag && index === ioIndex && (counter << 3) > ioOffset)
  // when sendReq or directRReq
  axiIO.AR.bits.addr := MuxCase(
    addr,
    Seq(
      cacheFSM.is(sendReq) -> Cat(Seq(tag, index, 0.U((log2Ceil(slotsPerLine) + 3).W))),
      cacheFSM.is(directRReq) -> addr,
      (cacheFSM.is(idle) && !shoudDirectRW) -> Cat(ioTag, ioIndex, 0.U((log2Ceil(slotsPerLine) + 3).W)),
      (cacheFSM.is(idle) && shoudDirectRW) -> io.addr
    )
  )
  axiIO.AR.bits.id   := 0.U
  axiIO.AR.bits.prot := 0.U
  axiIO.AR.valid := cacheFSM.is(sendReq) || cacheFSM.is(directRReq) ||
    (cacheFSM.is(idle) && io.readReq.fire && !hit && (shoudDirectRW || !isDirty)) ||
    (cacheFSM.is(idle) && io.writeReq.fire && !hit && !shoudDirectRW && !isDirty)
  axiIO.AR.bits.len := Mux(cacheFSM.is(sendReq) || (cacheFSM.is(idle) && !shoudDirectRW), (slotsPerLine - 1).U, 0.U)
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
  axiIO.R.ready := cacheFSM.is(waitRes) || cacheFSM.is(directRRes)
  // when writeData
  val dataWriteReq = Reg(io.writeReq.bits.cloneType)
  dataWriteReq := Mux(io.writeReq.fire, io.writeReq.bits, dataWriteReq)
  val extendedMask = Reverse(Cat(Seq.tabulate(dataWidth / 8)(index => Fill(8, dataWriteReq.mask(index)))))
  // ...1111110011111...
  val writeMask       = ~((extendedMask << (offset * 8.U)) | 0.U((cellByte * 8).W))
  val maskedWriteData = (dataWriteReq.data & extendedMask) << (offset * 8.U)
  for (i <- 0 until wayCnt) {
    when(cacheFSM.is(writeData) && index === i.U && cacheMem(i)(targetIndex).valid) {
      cacheMem(i)(targetIndex).data  := maskedWriteData | (cacheMem(i)(targetIndex).data & writeMask)
      cacheMem(i)(targetIndex).dirty := true.B
    }
  }
  // when sendWReq or directWReq
  axiIO.AW.valid := cacheFSM.is(sendWReq) || cacheFSM.is(directWReq) ||
    (cacheFSM.is(idle) && io.readReq.fire && !shoudDirectRW && !hit && isDirty) ||
    (cacheFSM.is(idle) && io.writeReq.fire && !shoudDirectRW && !hit && isDirty) ||
    (cacheFSM.is(idle) && io.writeReq.fire && shoudDirectRW)
  axiIO.AW.bits.addr := MuxCase(
    addr,
    Seq(
      cacheFSM.is(sendWReq) ->
        Cat(cacheMem(index)(replaceIndex).tag, index, 0.U((log2Ceil(slotsPerLine) + 3).W)),
      cacheFSM.is(directWReq) -> addr,
      (cacheFSM.is(idle) && !shoudDirectRW) -> Cat(
        cacheMem(ioIndex)(replaceIndeices(ioIndex)).tag,
        ioIndex,
        0.U((log2Ceil(slotsPerLine) + 3).W)
      ),
      (cacheFSM.is(idle) && shoudDirectRW) -> io.addr
    )
  )
  axiIO.AW.bits.len := Mux(cacheFSM.is(sendWReq) || (cacheFSM.is(idle) && !shoudDirectRW), (slotsPerLine - 1).U, 0.U)
  axiIO.W.valid     := cacheFSM.is(sendWData) || cacheFSM.is(directWData)
  axiIO.W.bits.data := Mux(
    cacheFSM.is(sendWData),
    PriorityMux(
      Seq.tabulate(slotsPerLine)(i =>
        ((i.U === counter) -> cacheMem(index)(replaceIndex).data((i + 1) * dataWidth - 1, i * dataWidth))
      )
    ),
    dataWriteReq.data
  )
  axiIO.W.bits.strb := Mux(cacheFSM.is(sendWData), Fill(8, true.B), dataWriteReq.mask)
  //when  waitWRes or directWRes
  axiIO.B.ready := cacheFSM.is(waitWRes) || cacheFSM.is(directWRes)

  axiIO.AW.bits.id    := DontCare
  axiIO.AW.bits.prot  := DontCare
  axiIO.AW.bits.burst := 2.U
  axiIO.AR.bits.burst := 2.U

  when(enableDebug) {
    when(cacheFSM.is(idle)) {
      val addr = io.addr
      when(io.writeReq.fire) {
        val data = io.writeReq.bits.data
        printf(
          name + " writing, addr is %x, mask is %x, tag is %x, index is %x, offset is %x, data is %x\n, pc is %x, inst is %x\n",
          addr,
          dataWriteReq.mask,
          tag,
          index,
          offset,
          data,
          io.debug.pc,
          io.debug.inst
        )
      }
      when(io.readReq.fire) {
        printf(
          name + " reading, addr is %x, tag is %x, index is %x, offset is %x\n, pc is %x, inst is %x\n",
          addr,
          tag,
          index,
          offset,
          io.debug.pc,
          io.debug.inst
        )
      }
    }
    when(io.data.fire) {
      printf("data is %x\n", io.data.bits)
    }
  }
  blackBoxCache.io.changed  := RegNext(!cacheFSM.is(idle)) && cacheFSM.is(idle)
  blackBoxCache.io.clock    := clock
  blackBoxCache.io.isDCache := name.equals("dcache").B
  blackBoxCache.io.reqValid := io.readReq.fire || io.writeReq.fire
  blackBoxCache.io.reqWrite := io.writeReq.fire
  blackBoxCache.io.isHit    := hit
}
