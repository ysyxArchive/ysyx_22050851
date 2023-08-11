import chisel3._
import chisel3.util.{is, switch, MuxLookup}
import chisel3.util.Decoupled
import decode.DecodeControlOut

object ControlRegisterList {
  def ControlRegister(name: String, id: Int, initVal: Int = 0) = id -> RegInit(initVal.U(64.W))

  val list = List(
    ControlRegister("satp", 0x180),
    ControlRegister("mstatus", 0x300),
    ControlRegister("mtvec", 0x305),
    ControlRegister("mscratch", 0x340),
    ControlRegister("mepc", 0x341),
    ControlRegister("mcause", 0x342)
  )
}

class ControlRegisterFileIO extends Bundle {
  val src1     = Input(UInt(64.W))
  val csrIndex = Input(UInt(12.W))
  val uimm     = Input(UInt(5.W))
  val control  = Input(new DecodeControlOut())
  val output   = Output(UInt(64.W))
}

class ControlRegisterFile extends Module {
  val io = IO(new ControlRegisterFileIO())

  val registers = ControlRegisterList.list((a, b) => (a, RegInit(b.U(64.W))))

  val writeBack = Wire(64.W)

  for (i <- 0 to ControlRegister.list.len) {
    ControlRegister.list(i) := Mux(io.csrIndex === i.U, writeBack, ControlRegister.list(i))
  }


  io.output := MuxLookup(
    csrIndex,
    DontCare,
    ControlRegisterList.list
  )

}
