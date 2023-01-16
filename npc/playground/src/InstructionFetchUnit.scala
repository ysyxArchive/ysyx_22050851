import Chisel.{Cat, Decoupled, DecoupledIO, Fill, Mux1H, MuxCase, MuxLookup, switch}
import chisel3.DontCare.:=
import chisel3._
import chisel3.experimental.{
  ChiselEnum, BaseModule
}
import chisel3.util.Enum

class InstructionFetchUnit {
  val in = Flipped(Decoupled(new Bundle {
    val addr = UInt(64.W)
  }))
  val fetch = Decoupled(new Bundle {
    val inst = UInt(32.W)
  })
  val out = Decoupled(new Bundle {
    val inst = UInt(32.W)
  })
//   in.ready := true.B
}