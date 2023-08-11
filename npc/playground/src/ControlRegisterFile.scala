import chisel3._
import chisel3.util.{is, switch, MuxLookup}

class ControlRegisterInfo(val name: String, val id: Int, val initVal: Int = 0) {}

object ControlRegisterList {
  val list = List(
    new ControlRegisterInfo("satp", 0x180),
    new ControlRegisterInfo("mstatus", 0x300),
    new ControlRegisterInfo("mtvec", 0x305),
    new ControlRegisterInfo("mscratch", 0x340),
    new ControlRegisterInfo("mepc", 0x341),
    new ControlRegisterInfo("mcause", 0x342)
  )
}

class ControlRegisterFileIO extends Bundle {
  val src1     = Input(UInt(64.W))
  val csrIndex = Input(UInt(12.W))
  val uimm     = Input(UInt(5.W))
  val output   = Output(UInt(64.W))
}

class ControlRegisterFile extends Module {
  val io = IO(new ControlRegisterFileIO())

  val registers = ControlRegisterList.list.map(info -> RegInit(info.initVal.U(64.W)))

}
