import chisel3._
import chisel3.util._
import utils.FSM
import decode.AluMode

class CacheIO extends Bundle {
  val readReq = Decoupled(UInt(64.W))
  val data    = Flipped(Decoupled(UInt(32.W)))
}

class CacheLine(tagWidth: Int, dataByte: Int) extends Bundle {
  val valid = RegInit(false.B)
  val tag   = Reg(UInt(tagWidth.W))
  val data  = Reg(UInt((dataByte * 8).W))

}

/**
  * @param totalByte 整个Cache字节数
  * @param cellByte 单个cell字节数
  * @param groupSize 单组有多少个cell
  */
class Cache(totalByte: Int, groupSize: Int, addrWidth: Int = 64) extends Module {
  val cellByte = 8
  assert(totalByte % cellByte == 0)
  val cellCnt = totalByte / cellByte
  assert(cellCnt % groupSize == 0)
  val waycnt      = cellCnt / groupSize
  val indexOffset = log2Up(cellByte)
  val tagOffset   = log2Up(cellByte) + log2Up(groupSize)

  val io    = IO(new CacheIO())
  val axiIO = IO(new AxiLiteIO(UInt(64.W), 64))
  val cacheMem = Vec(waycnt, Vec(groupSize, new CacheLine(addrWidth - tagOffset, cellByte)))
  // val cacheMem = Seq.tabulate(waycnt)(_ => Seq.tabulate(groupSize)(_ => new CacheLine(addrWidth - tagOffset, cellByte)))

  val hit = Wire(Bool())

  val idle :: sendRes :: sendReq :: waitRes :: others = Enum(5)

  val cacheFSM = new FSM(
    idle,
    List(
      (idle, io.data.fire && hit, sendRes),
      (sendRes, io.data.fire, idle),
      (idle, io.data.fire && !hit, sendReq),
      (sendReq, false.B, waitRes),
      (waitRes, false.B, sendRes)
    )
  )

  val tag    = io.readReq.bits(addrWidth - 1, tagOffset)
  val index  = io.readReq.bits(tagOffset - 1, indexOffset)
  val offset = io.readReq.bits(indexOffset - 1, 0)

  val wayValid = cacheMem(index).map(line => line.valid && line.tag === tag)

  hit := wayValid.reduce(_ & _)

  // when idle
  val addr = Reg(UInt(addrWidth.W))
  addr          := Mux(cacheFSM.is(idle), io.data.bits, addr)
  io.data.ready := cacheFSM.is(idle) && io.data.valid
  // when sendRes
  val data = Mux1H(wayValid, cacheMem(index)).data
  val s    = Seq.tabulate(cellByte)(o => ((o.U === offset) -> data(o * 8 - 1, 0)))
  io.data.bits  := PriorityMux(s)
  io.data.valid := true.B
  // when sendReq
  axiIO.AR.bits  := io.readReq.bits
  axiIO.AR.valid := Mux(cacheFSM.is(sendReq), io.data.bits, addr)
  // when waitRes
  for (i <- 0 to waycnt) {
    when(cacheFSM.is(waitRes) && index === i.U && axiIO.R.fire) {
      cacheMem(i)(0).data  := axiIO.R.bits
      cacheMem(i)(0).tag   := tag
      cacheMem(i)(0).valid := true.B
    }
  }
}
