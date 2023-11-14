import chisel3._
import chisel3.util.{is, switch, MuxLookup}
import chisel3.util.Decoupled
import decode.CsrSource
import decode.AluMux1
import decode._
import utils._
import chisel3.util.Fill
import chisel3.internal.firrtl.Index
import chisel3.util.MuxCase

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

  val registers = ControlRegisters.list.map(info => RegInit(info.initVal.U(64.W)))

  def apply(id: UInt): UInt = MuxLookup(id, 0.U)(
    ControlRegisters.list.zipWithIndex.map {
      case (info, index) => info.id.U -> registers(index)
    }.toSeq
  )

  def apply(name: String): UInt = apply(ControlRegisters.getInfoByName(name).id.U)

  def set(name: String, value: UInt) = registers(ControlRegisters.getIndexByName(name)) := value

}

object ControlRegisters {
  class ControlRegisterInfo(val name: String, val id: Int, val initVal: Int = 0)
  val list = List(
    new ControlRegisterInfo("mepc", 0x341),
    new ControlRegisterInfo("mstatus", 0x300, 0x1800),
    new ControlRegisterInfo("mcause", 0x342),
    new ControlRegisterInfo("mtvec", 0x305),
    new ControlRegisterInfo("satp", 0x180),
    new ControlRegisterInfo("mscratch", 0x340)
  )

  def getIndexByName(name: String) = list.indexWhere(info => { info.name == name })

  def getInfoByName(name: String) = list(getIndexByName(name))

  def namesToId(names: Seq[String]) = names.map(name => getInfoByName(name).id.U(12.W))

  def behaveDependency(behave: UInt, setMode: UInt, imm: UInt) =
    MuxLookup(behave, VecInit.fill(3)(0.U(12.W)))(
      EnumSeq(
        CsrBehave.ecall -> VecInit(namesToId(Seq("mepc", "mstatus", "mcause"))),
        CsrBehave.mret -> VecInit(namesToId(Seq("mstatus")) ++ Seq(0.U(12.W), 0.U(12.W))),
        CsrBehave.no -> Mux(
          setMode =/= CsrSetMode.origin.asUInt,
          VecInit(imm(11, 0), 0.U(12.W), 0.U(12.W)),
          VecInit.fill(3)(0.U(12.W))
        )
      )
    )

  def behaveReadDependency(behave: UInt) =
    MuxLookup(behave, 0.U(12.W))(
      EnumSeq(
        CsrBehave.ecall -> getInfoByName("mtvec").id.U(12.W),
        CsrBehave.mret -> getInfoByName("mepc").id.U(12.W)
      )
    )
}

object PrivMode {
  val U = 0.U
  val S = 1.U
  val V = 2.U
  val M = 3.U
}
class CSRFileControl extends Bundle {
  val csrBehave  = Input(UInt(CsrBehave.getWidth.W))
  val csrSource  = Input(UInt(CsrSource.getWidth.W))
  val csrSetmode = Input(UInt(CsrSetMode.getWidth.W))
}

class ControlRegisterFileControlIO extends Bundle {
  val data    = Flipped(new WBDataIn())
  val control = new CSRFileControl()
  val output  = Output(UInt(64.W))
}
class ControlRegisterFileDataIO extends Bundle {
  val output    = Output(UInt(64.W))
  val csrBehave = Input(UInt(CsrBehave.getWidth.W))
}

class ControlRegisterFile extends Module {
  val controlIO = IO(new ControlRegisterFileControlIO())
  val dataIO    = IO(new ControlRegisterFileDataIO())
  val debugOut  = IO(Output(Vec(6, UInt(64.W))))

  val uimm  = controlIO.data.src1
  val csrId = controlIO.data.imm

  val register = new ControlRegisters()

  debugOut := register.registers

  val mstatus = new Mstatus(register("mstatus"))

  val currentMode = RegInit(PrivMode.M)
  currentMode := MuxLookup(controlIO.control.csrBehave, currentMode)(
    EnumSeq(CsrBehave.ecall -> PrivMode.M, CsrBehave.mret -> mstatus("MPP"))
  )

  val mask = MuxLookup(controlIO.control.csrSource, controlIO.data.src1Data)(
    EnumSeq(
      CsrSource.src1 -> controlIO.data.src1Data,
      CsrSource.uimm -> uimm
    )
  )
  val writeBack = Wire(UInt(64.W))
  val outputVal = register(csrId)

  for (i <- 0 to register.registers.length - 1) {
    val name = ControlRegisters.list(i).name
    val id   = ControlRegisters.list(i).id
    name match {
      case "mstatus" => {
        register.set(
          "mstatus",
          MuxLookup(
            controlIO.control.csrBehave,
            Mux(csrId === id.U, writeBack, register("mstatus"))
          )(
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
            controlIO.control.csrBehave === CsrBehave.ecall.asUInt,
            controlIO.data.pc,
            Mux(csrId === id.U, writeBack, register("mepc"))
          )
        )
      }
      case "mcause" => {
        register.set(
          "mcause",
          Mux(
            controlIO.control.csrBehave === CsrBehave.ecall.asUInt,
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

  writeBack := MuxLookup(controlIO.control.csrSetmode, outputVal)(
    EnumSeq(
      CsrSetMode.clear -> (outputVal & ~mask),
      CsrSetMode.set -> (outputVal | mask),
      CsrSetMode.write -> mask
    )
  )

  controlIO.output := MuxLookup(controlIO.control.csrBehave, 0.U)(
    EnumSeq(
      CsrBehave.no -> outputVal,
      CsrBehave.ecall -> register("mtvec"),
      CsrBehave.mret -> register("mepc")
    )
  )

  dataIO.output := MuxLookup(dataIO.csrBehave, 0.U)(
    EnumSeq(
      CsrBehave.ecall -> register("mtvec"),
      CsrBehave.mret -> register("mepc")
    )
  )
}
