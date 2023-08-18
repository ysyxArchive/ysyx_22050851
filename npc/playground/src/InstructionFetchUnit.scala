import Chisel.{switch, Cat, Decoupled, DecoupledIO, Fill, Mux1H, MuxCase, MuxLookup}
import chisel3.DontCare.:=
import chisel3._
import chisel3.experimental.{BaseModule, ChiselEnum}
import chisel3.util.Enum
import dataclass.data

class FetchDecodeAxiIO extends AxiLiteReadIO(32, 64) {}

class InstructionFetchUnit extends Module {
  val instOut = IO(Flipped(new FetchDecodeAxiIO()))

  val mem = Module(new BlackBoxMem)

  val instValid = RegInit(false.B)
  val inst      = RegInit(0.U(32.W))

  val outData = Wire(new FetchDecodeAxiIO().R.bits)
  outData.id   := 0.U
  outData.data := inst

  mem.io.clock      := clock
  mem.io.isRead     := true.B
  mem.io.isUnsigned := true.B
  mem.io.len        := 8.U
  mem.io.addr       := instOut.AR.bits.addr
  mem.io.enable     := instOut.AR.fire

  inst := Mux(instOut.AR.fire, mem.io.rdata, inst)

  instValid := Mux(instValid, instOut.R.fire, instOut.AR.fire)

  instOut.AR.ready := !instValid
  instOut.R.valid  := instValid

  instOut.R.bits := outData

}
