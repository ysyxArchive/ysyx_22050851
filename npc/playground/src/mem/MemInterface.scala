import chisel3._
import chisel3.experimental.IO
import chisel3.util._
import os.stat
import utils.FSM
object MemAxiLite {
  def apply() = AxiLiteIO(64, 64)
}

object MemReadOnlyAxiLiteIO {
  def slave() = {
    val io = IO(Flipped(MemAxiLite()))
    io.B.valid   := false.B
    io.B.bits.id := DontCare
    io.W.ready   := false.B
    io.AW.ready  := false.B
    io
  }
  def master() = {
    val io = IO(MemAxiLite())
    io.B.ready      := false.B
    io.W.bits.data  := DontCare
    io.AW.bits.id   := DontCare
    io.AW.bits.prot := DontCare
    io.W.valid      := false.B
    io.AW.valid     := false.B
    io.W.bits.strb  := DontCare
    io.AW.bits.addr := DontCare
    io
  }
}

class MemInterface extends Module {
  val axiS = IO(Flipped(MemAxiLite()))
  val mem  = Module(new BlackBoxMem)

  val waitReq :: writeBack :: others = Enum(3)

  val status = RegInit(waitReq)
  status := FSM(
    status,
    List(
      (waitReq, axiS.AR.fire || (axiS.AW.fire && axiS.W.fire), writeBack),
      (writeBack, axiS.R.fire || axiS.B.fire, waitReq)
    )
  )

  val dataRet    = RegInit(0.U(64.W))
  val isReading  = RegInit(false.B)
  val readResId  = Reg(UInt(axiS.id_r_width.W))
  val writeResId = Reg(UInt(axiS.id_w_width.W))

  // now rather read or write
  val readValid    = axiS.AR.valid
  val writeValid   = axiS.AW.valid && axiS.W.valid
  val requestValid = readValid || writeValid

  // AW W always fire together
  val writeReqFire = axiS.AW.fire && axiS.W.fire
  val readReqFire  = axiS.AR.fire
  val reqFire      = writeReqFire || readReqFire

  mem.io.clock  := clock
  mem.io.isRead := readReqFire
  mem.io.mask   := axiS.W.bits.strb
  mem.io.wdata  := axiS.W.bits.data
  mem.io.addr   := Mux(readReqFire, axiS.AR.bits.addr, axiS.AW.bits.addr)
  mem.io.enable := reqFire
  dataRet       := Mux(reqFire, mem.io.rdata, dataRet)

  // interface status
  isReading  := Mux(status === writeBack, isReading, axiS.AR.valid)
  readResId  := Mux(status === writeBack, readResId, axiS.AR.bits.id)
  writeResId := Mux(status === writeBack, writeResId, axiS.AW.bits.id)

  axiS.W.ready     := status === waitReq && !axiS.AR.valid && axiS.W.valid
  axiS.AW.ready    := status === waitReq && !axiS.AR.valid && axiS.AW.valid
  axiS.AR.ready    := status === waitReq && axiS.AR.valid
  axiS.B.valid     := status === writeBack && !isReading
  axiS.B.bits.id   := writeResId
  axiS.R.valid     := status === writeBack && isReading
  axiS.R.bits.id   := readResId
  axiS.R.bits.data := dataRet
}
