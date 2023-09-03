import chisel3._
import chisel3.util.{is, switch, MuxLookup}
import chisel3.util.Decoupled
import decode.DecodeControlOut
import decode.CsrSource
import decode.AluMux1
import firrtl.seqCat
import decode._
import Chisel.debug
import utils._
import chisel3.util.Fill
import chisel3.internal.firrtl.Index

class Mstatus(val value: UInt) {
  class OffsetWidth(val offset: Int, val width: Int) {
    val offsetFromBegin = offset + width
  }
  val map = Map(
    "SD" -> new OffsetWidth(63, 1),
    "MPV" -> new OffsetWidth(39, 1),
    "GVA" -> new OffsetWidth(38, 1),
    "MBE" -> new OffsetWidth(37, 1),
    "SBE" -> new OffsetWidth(36, 1),
    "SXL" -> new OffsetWidth(34, 2),
    "UXL" -> new OffsetWidth(32, 2),
    "TSR" -> new OffsetWidth(22, 1),
    "TW" -> new OffsetWidth(21, 1),
    "TVM" -> new OffsetWidth(20, 1),
    "MXR" -> new OffsetWidth(19, 1),
    "SUM" -> new OffsetWidth(18, 1),
    "MPRV" -> new OffsetWidth(17, 1),
    "XS" -> new OffsetWidth(15, 2),
    "FS" -> new OffsetWidth(13, 2),
    "MPP" -> new OffsetWidth(11, 2),
    "VS" -> new OffsetWidth(9, 2),
    "SPP" -> new OffsetWidth(8, 1),
    "MPIE" -> new OffsetWidth(7, 1),
    "UBE" -> new OffsetWidth(6, 1),
    "SPIE" -> new OffsetWidth(5, 1),
    "MIE" -> new OffsetWidth(3, 1),
    "SIE" -> new OffsetWidth(1, 1)
  )
  def getMask(name: String) = {
    val target = map(name)
    Utils.zeroExtend(Fill(target.width, 1.U) << target.offset, target.offsetFromBegin, 64)
  }

  def getSettledValue(pairs: (String, UInt)*) = {
    val mask     = pairs.map(pair => ~getMask(pair._1)).reduce(_ & _)
    val setValue = pairs.map(pair => (pair._2(map(pair._1).width - 1, 0) << map(pair._1).offset)).reduce(_ | _)
    value & mask | setValue
  }

  def get(name: String) = {
    val target = map(name)
    (value & getMask(name)) >> target.offset
  }

  def apply(name: String) = get(name)
}

class ControlRegisters {
  class ControlRegisterInfo(val name: String, val id: Int, val initVal: Int = 0)
  val list = List(
    new ControlRegisterInfo("mepc", 0x341),
    new ControlRegisterInfo("mstatus", 0x300, 0x1800),
    new ControlRegisterInfo("mcause", 0x342),
    new ControlRegisterInfo("mtvec", 0x305),
    new ControlRegisterInfo("satp", 0x180),
    new ControlRegisterInfo("mscratch", 0x340)
  )

  val registers = list.map(info => RegInit(info.initVal.U(64.W)))

  def getIndexByName(name: String) = list.indexWhere(info => { info.name == name })

  def getInfoByName(name: String) = list(getIndexByName(name))

  def apply(id: UInt): UInt =
    MuxLookup(
      id,
      0.U,
      list.zipWithIndex.map {
        case (info, index) => info.id.U -> registers(index)
      }.toSeq
    )

  def apply(name: String): UInt = apply(getInfoByName(name).id.U)

  def set(name: String, value: UInt) = {
    val index = getIndexByName(name)
    registers(index) := value
  }
}

object PrivMode {
  val U = 0.U
  val S = 1.U
  val V = 2.U
  val M = 3.U
}

class ControlRegisterFileIO extends Bundle {
  val src1Data = Input(UInt(64.W))
  val decodeIn = Input(Flipped(new DecodeOut()))
  val output   = Output(UInt(64.W))
}

class ControlRegisterFile extends Module {
  val io       = IO(new ControlRegisterFileIO())
  val debugOut = IO(Output(Vec(6, UInt(64.W))))
  val regIn    = IO(Input(Flipped(new RegisterFileIO())))

  val uimm  = io.decodeIn.data.src1
  val csrId = io.decodeIn.data.imm

  val register = new ControlRegisters()

  debugOut := register.registers

  val mstatus = new Mstatus(register("mstatus"))

  val currentMode = RegInit(PrivMode.M)
  currentMode := MuxLookup(
    io.decodeIn.control.csrbehave,
    currentMode,
    EnumSeq(CsrBehave.ecall -> PrivMode.M, CsrBehave.mret -> mstatus("MPP"))
  )

  val mask = MuxLookup(
    io.decodeIn.control.csrsource,
    io.src1Data,
    EnumSeq(
      CsrSource.src1 -> io.src1Data,
      CsrSource.uimm -> uimm
    )
  )
  val writeBack = Wire(UInt(64.W))
  val outputVal = register(csrId)

  for (i <- 0 to register.registers.length - 1) {
    val name = register.list(i).name
    val id   = register.list(i).id
    name match {
      case "mstatus" => {
        register.set(
          "mstatus",
          MuxLookup(
            io.decodeIn.control.csrbehave,
            Mux(csrId === id.U, writeBack, register("mstatus")),
            EnumSeq(
              CsrBehave.ecall -> mstatus.getSettledValue("MPP" -> currentMode, "MPIE" -> mstatus("MIE"), "MIE" -> 0.U),
              CsrBehave.mret -> mstatus.getSettledValue("MIE" -> mstatus("MPIE"), "MPIE" -> 1.U, "MPP" -> PrivMode.U)
            )
          )
        )
      }
      case "mepc" => {
        register.set(
          "mepc",
          Mux(
            io.decodeIn.control.csrbehave === CsrBehave.ecall.asUInt,
            regIn.pc,
            Mux(csrId === id.U, writeBack, register("mepc"))
          )
        )
      }
      case "mcause" => {
        register.set(
          "mcause",
          Mux(
            io.decodeIn.control.csrbehave === CsrBehave.ecall.asUInt,
            Mux(currentMode === PrivMode.U, 0x8.U, 0xb.U),
            Mux(csrId === id.U, writeBack, register("mcause"))
          )
        )
      }
      case _ => {
        register.set(name, Mux(csrId === id.U, writeBack, register(name)))
      }
    }
  }

  writeBack := MuxLookup(
    io.decodeIn.control.csrsetmode,
    outputVal,
    EnumSeq(
      CsrSetMode.clear -> (outputVal & ~mask),
      CsrSetMode.set -> (outputVal | mask),
      CsrSetMode.write -> mask
    )
  )

  io.output := MuxLookup(
    io.decodeIn.control.csrbehave,
    outputVal,
    EnumSeq(
      CsrBehave.no -> outputVal,
      CsrBehave.ecall -> register("mtvec"),
      CsrBehave.mret -> register("mepc")
    )
  )

}
