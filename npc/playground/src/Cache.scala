import chisel3._
import chisel3.util._
import utils.FSM
import decode.AluMode

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
  // val cacheMem = Vec(waycnt, Vec(groupSize, Wire(new CacheLine(addrWidth - tagOffset, cellByte))))
  val cacheMem = RegInit(
    VecInit(Seq.fill(waycnt)(VecInit(Seq.fill(groupSize)(Wire(new CacheLine(addrWidth - tagOffset, cellByte))))))
  )

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
  addr             := Mux(cacheFSM.is(idle), io.readReq.bits, addr)
  io.readReq.ready := cacheFSM.is(idle) && io.readReq.valid
  // when sendRes
  val data = Mux1H(wayValid, cacheMem(index)).data
  val s    = Seq.tabulate(cellByte - 1)(o => (((o + 1).U === offset) -> data((o + 1) * 8 - 1, 0)))
  io.data.bits  := PriorityMux(s)
  io.data.valid := true.B
  // when sendReq
  axiIO.AR.bits.addr := io.readReq.bits
  axiIO.AR.bits.id   := 0.U
  axiIO.AR.bits.prot := 0.U
  axiIO.AR.valid     := Mux(cacheFSM.is(sendReq), io.data.bits, addr)
  // when waitRes
  for (i <- 0 until waycnt) {
    when(cacheFSM.is(waitRes) && index === i.U && axiIO.R.fire) {
      cacheMem(i)(0).data  := axiIO.R.bits.data
      cacheMem(i)(0).tag   := tag
      cacheMem(i)(0).valid := true.B
    }
  }

  axiIO.AW.valid     := DontCare
  axiIO.AW.bits.id   := DontCare
  axiIO.AW.bits.addr := DontCare
  axiIO.AW.bits.prot := DontCare
  axiIO.W.valid      := DontCare
  axiIO.W.bits.data  := DontCare
  axiIO.W.bits.strb  := DontCare
  axiIO.B.ready      := DontCare
  axiIO.R.ready      := DontCare
}
