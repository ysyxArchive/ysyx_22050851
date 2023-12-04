<<<<<<< HEAD
import chisel3._
import chisel3.util._
import utils.FSM
=======
package mem

import chisel3._
import chisel3.util._
import utils.FSM

>>>>>>> adaab1e8590675071c22ec50f610816123747f3a
object MemAxiLite {
  def apply() = AxiLiteIO(64, 64)
}

object MemBurstAxiLite {
  def apply() = BurstLiteIO(64, 64)
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

  val memInterfaceFSM = new FSM(
    waitReq,
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
  isReading  := Mux(memInterfaceFSM.is(writeBack), isReading, axiS.AR.valid)
  readResId  := Mux(memInterfaceFSM.is(writeBack), readResId, axiS.AR.bits.id)
  writeResId := Mux(memInterfaceFSM.is(writeBack), writeResId, axiS.AW.bits.id)

  axiS.W.ready     := memInterfaceFSM.is(waitReq) && !axiS.AR.valid && axiS.W.valid
  axiS.AW.ready    := memInterfaceFSM.is(waitReq) && !axiS.AR.valid && axiS.AW.valid
  axiS.AR.ready    := memInterfaceFSM.is(waitReq) && axiS.AR.valid
  axiS.B.valid     := memInterfaceFSM.is(writeBack) && !isReading
  axiS.B.bits.id   := writeResId
  axiS.R.valid     := memInterfaceFSM.is(writeBack) && isReading
  axiS.R.bits.id   := readResId
  axiS.R.bits.data := dataRet
}

class MemBurstInterface extends Module {
  val axiS = IO(Flipped(MemBurstAxiLite()))
  val mem  = Module(new BlackBoxMem)

  val waitReq :: writeDataBack :: waitDataWrite :: responseWrite :: others = Enum(5)

  val counter = RegInit(0.U(9.W))

  val writeReq = Reg(MemBurstAxiLite().AW.bits)
  val readReq  = Reg(MemBurstAxiLite().AR.bits)

  val memInterfaceFSM = new FSM(
    waitReq,
    List(
      (waitReq, axiS.AR.fire, writeDataBack),
      (writeDataBack, axiS.R.fire && counter - 1.U === readReq.len, waitReq),
      (waitReq, axiS.AW.fire, waitDataWrite),
      (waitDataWrite, axiS.W.fire && counter === writeReq.len, responseWrite),
      (responseWrite, axiS.B.fire, waitReq)
    )
  )

  val dataRet = RegInit(0.U(64.W))

  writeReq := Mux(axiS.AW.fire, axiS.AW.bits, writeReq)
  readReq  := Mux(axiS.AR.fire, axiS.AR.bits, readReq)

  counter := MuxCase(
    counter,
    Seq(
      (memInterfaceFSM.is(waitReq) && !memInterfaceFSM.willChange()) -> 0.U,
      (memInterfaceFSM.willChangeTo(waitReq)) -> 0.U,
      (memInterfaceFSM.is(waitReq) && axiS.AR.fire) -> (counter + 1.U),
      (memInterfaceFSM.is(writeDataBack)) -> (counter + 1.U),
      (memInterfaceFSM.is(waitDataWrite) && axiS.W.fire) -> (counter + 1.U)
    )
  )

  mem.io.clock  := clock
  mem.io.isRead := memInterfaceFSM.is(writeDataBack) || (memInterfaceFSM.is(waitReq) && axiS.AR.valid)
  mem.io.mask   := axiS.W.bits.strb
  mem.io.wdata  := axiS.W.bits.data
  mem.io.addr := MuxCase(
    0.U,
    Seq(
      (memInterfaceFSM.is(waitReq) && axiS.AR.fire) -> axiS.AR.bits.addr,
      memInterfaceFSM.is(writeDataBack) -> readReq.addr,
      axiS.W.fire -> writeReq.addr
    )
  ) + (counter << 3)
  mem.io.enable :=
    (memInterfaceFSM.is(writeDataBack) && !memInterfaceFSM.willChange()) ||
      (memInterfaceFSM.is(waitReq) && axiS.AR.fire) ||
      (memInterfaceFSM.is(waitDataWrite) && axiS.W.fire)
  dataRet := Mux(mem.io.enable, mem.io.rdata, dataRet)

  axiS.W.ready     := memInterfaceFSM.is(waitDataWrite)
  axiS.AW.ready    := memInterfaceFSM.is(waitReq) && !axiS.AR.valid
  axiS.AR.ready    := memInterfaceFSM.is(waitReq)
  axiS.B.valid     := memInterfaceFSM.is(responseWrite)
  axiS.B.bits.id   := writeReq.id
  axiS.R.valid     := memInterfaceFSM.is(writeDataBack)
  axiS.R.bits.id   := readReq.id
  axiS.R.bits.data := dataRet
  axiS.R.bits.last := counter - 1.U === readReq.len
}
