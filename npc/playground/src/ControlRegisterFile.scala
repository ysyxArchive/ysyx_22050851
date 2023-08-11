import chisel3._
import chisel3.util.{is, switch, MuxLookup}
import chisel3.util.Decoupled
import decode.DecodeControlOut
import decode.CsrSource
import decode.AluMux1
import firrtl.seqCat
import decode.CsrSetMode

class ControlRegisterInfo(val name: String, val id: Int, val initVal: Int = 0)

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
  val control  = Input(new DecodeControlOut())
  val output   = Output(UInt(64.W))
}

class ControlRegisterFile extends Module {
  val io = IO(new ControlRegisterFileIO())

  val registers = ControlRegisterList.list.map(info => RegInit(info.initVal.U(64.W)))
  val indexMap  = ControlRegisterList.list.zipWithIndex.map { case (info, index) => info.id.U -> registers(index) }.toMap
  val mask = MuxLookup(
    io.control.csrsource,
    io.src1,
    Seq(
      CsrSource.src1.asUInt -> io.src1,
      CsrSource.uimm.asUInt -> io.uimm
    )
  )

  indexMap(io.csrIndex) := MuxLookup(
    io.control.csrsetmode,
    indexMap(io.csrIndex),
    Seq(
      CsrSetMode.clear.asUInt -> (indexMap(io.csrIndex) & ~mask),
      CsrSetMode.set.asUInt -> (indexMap(io.csrIndex) | mask),
      CsrSetMode.write.asUInt -> mask
    )
  )

  io.output := indexMap(io.csrIndex)

}
