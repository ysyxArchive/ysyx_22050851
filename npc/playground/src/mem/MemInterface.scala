import chisel3._

object MemAxiLite {
  def apply() = AxiLiteIO(32, 64)
}

class MemInterface extends Module {
  val axiS = IO(Flipped(MemAxiLite()))
  val mem  = Module(new BlackBoxMem)

  // now rather read or write
  val requestValid = axiS.AR.valid || axiS.AW.valid
  val isRead       = axiS.AR.valid

  mem.io.clock  := clock
  mem.io.isRead := isRead
  mem.io.mask   := axiS.W.bits.strb
  // mem.io.addr   := instOut.AR.bits.addr
  // mem.io.enable := instOut.AR.fire

  // inst := Mux(instOut.AR.fire, mem.io.rdata, inst)

  // instValid := Mux(instValid, !instOut.R.fire, instOut.AR.fire)

  // instOut.AR.ready := !instValid
  // instOut.R.valid  := instValid

  // instOut.R.bits := outData

}
