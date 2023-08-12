import chisel3._
import chisel3.util.{is, switch, MuxLookup}
import chisel3.util.Decoupled
import decode.DecodeControlOut
import decode.CsrSource
import decode.AluMux1
import firrtl.seqCat
import decode._
import Chisel.debug
import utils.Utils

class ControlRegisterInfo(val name: String, val id: Int, val initVal: Int = 0)

object ControlRegisterList {
  // 顺序和 csrc/regs.cpp 中 csrregs 相同
  val list = List(
    new ControlRegisterInfo("mepc", 0x341),
    new ControlRegisterInfo("mstatus", 0x300),
    new ControlRegisterInfo("mcause", 0x342),
    new ControlRegisterInfo("mtvec", 0x305),
    new ControlRegisterInfo("satp", 0x180),
    new ControlRegisterInfo("mscratch", 0x340)
  )

  def IndexOf(name: String) = list.indexWhere(info => { info.name == name })
}

object PrivMode {
  val U = 0.U
  val S = 1.U
  val V = 2.U
  val M = 3.U
}

class ControlRegisterFileIO extends Bundle {
  val src1Data = Input(UInt(64.W))
  val decodeIn = Flipped(new DecodeOut())
  val output   = Output(UInt(64.W))
}

class ControlRegisterFile extends Module {
  val io       = IO(new ControlRegisterFileIO())
  val debugOut = IO(Output(Vec(6, UInt(64.W))))
  val regIn    = IO(Flipped(new RegisterFileIO()))

  val uimm     = io.decodeIn.data.src1
  val csrIndex = io.decodeIn.data.imm

  val registers = ControlRegisterList.list.map(info => RegInit(info.initVal.U(64.W)))
  debugOut := registers
  val indexMapSeq = ControlRegisterList.list.zipWithIndex.map {
    case (info, index) => info.id.U -> registers(index)
  }.toSeq
  // TODO:Impl
  val privMode = 0.U(2.W)

  val mask = MuxLookup(
    io.decodeIn.control.csrsource,
    io.src1Data,
    Seq(
      CsrSource.src1.asUInt -> io.src1Data,
      CsrSource.uimm.asUInt -> uimm
    )
  )
  val writeBack = Wire(UInt(64.W))
  val outputVal = MuxLookup(csrIndex, 0.U, indexMapSeq)
  for (i <- 0 to registers.length - 1) {
    ControlRegisterList.list(i).name match {
      // case "mstatus" => {
      //   registers(i) := Mux(

      //   )
      // }
      case "mepc" => {
        registers(i) := Mux(
          io.decodeIn.control.csrbehave === CsrBehave.ecall.asUInt,
          regIn.pc,
          Mux(csrIndex === ControlRegisterList.list(i).id.U, writeBack, registers(i))
        )
      }
      case "mcause" => {
        registers(i) := Mux(
          io.decodeIn.control.csrbehave === CsrBehave.ecall.asUInt,
          Mux(privMode === PrivMode.U, 0x8.U, 0xb.U),
          Mux(csrIndex === ControlRegisterList.list(i).id.U, writeBack, registers(i))
        )
      }
      case _ => {
        registers(i) := Mux(csrIndex === ControlRegisterList.list(i).id.U, writeBack, registers(i))
      }
    }
  }

  writeBack := MuxLookup(
    io.decodeIn.control.csrsetmode,
    outputVal,
    Seq(
      CsrSetMode.clear.asUInt -> (outputVal & ~mask),
      CsrSetMode.set.asUInt -> (outputVal | mask),
      CsrSetMode.write.asUInt -> mask
    )
  )

  io.output := MuxLookup(
    io.decodeIn.control.csrbehave,
    outputVal,
    Utils.enumSeq(
      CsrBehave.no -> outputVal,
      CsrBehave.ecall -> registers(ControlRegisterList.IndexOf("mtvec")),
      CsrBehave.mret -> registers(ControlRegisterList.IndexOf("mepc"))
    )
  )

}
