import chisel3._
import chisel3.util._
import utils.FSM
import decode.AluMode
import os.group

class CacheIO extends Bundle {
  val readReq = Flipped(Decoupled(UInt(64.W)))
  val data    = Decoupled(UInt(32.W))
}

class CacheLine(tagWidth: Int, dataByte: Int) extends Bundle {
  val valid = Bool()
  val tag   = UInt(tagWidth.W)
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

  val hit = Wire(Bool())

  val idle :: sendRes :: sendReq :: waitRes :: others = Enum(5)

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
      (sendRes, io.data.fire, idle),
      (idle, io.readReq.fire && !hit, sendReq),
      (sendReq, axiIO.AR.fire, waitRes),
      (waitRes, axiIO.R.fire && (counter =/= (updateTimes - 1).U), sendReq),
      (waitRes, axiIO.R.fire && (counter === (updateTimes - 1).U), sendRes)
    )
  )

  val replaceIndex = RegInit(0.U(log2Ceil(groupSize).W))

  val tag    = io.readReq.bits(addrWidth - 1, tagOffset)
  val index  = io.readReq.bits(tagOffset - 1, indexOffset)
  val offset = io.readReq.bits(indexOffset - 1, 0)

  val wayValid = cacheMem(index).map(line => line.valid && line.tag === tag)

  hit := wayValid.reduce(_ & _)
  
  // when idle
  val addr = Reg(UInt(addrWidth.W))
  addr             := Mux(cacheFSM.is(idle), io.readReq.bits, addr)
  io.readReq.ready := cacheFSM.is(idle) && io.readReq.valid
  replaceIndex     := Mux(cacheFSM.is(idle), Mux(replaceIndex === (groupSize - 1).U, 0.U, replaceIndex + 1.U), replaceIndex)
  // when sendRes
  val line = Mux1H(wayValid, cacheMem(index))
  val data = line.data
  val s    = Seq.tabulate(cellByte)(o => ((o.U === offset) -> data(data.getWidth - 1, o * 8)))
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

  axiIO.AW.valid     := false.B
  axiIO.W.valid      := false.B
  axiIO.B.ready      := false.B
  axiIO.AW.bits.id   := DontCare
  axiIO.AW.bits.addr := DontCare
  axiIO.AW.bits.prot := DontCare
  axiIO.W.bits.data  := DontCare
  axiIO.W.bits.strb  := DontCare
}
