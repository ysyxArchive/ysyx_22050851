import chisel3._
import chisel3.experimental.BundleLiterals._
import chisel3.util._
import utils._
import decode._
import execute._

class DecodeIn extends Bundle {
  val debug = Output(new DebugInfo)
  val pc    = Output(UInt(64.W))
  val inst  = Output(UInt(32.W))
}

object DecodeIn {
  def default = {
    val defaultWire = WireDefault(0.U.asTypeOf(new DecodeIn()))
    defaultWire.pc := "h80000000".asUInt(64.W)
    defaultWire
  }
}

class DecodeBack extends Bundle {
  val valid          = Output(Bool())
  val willTakeBranch = Output(Bool())
  val branchPc       = Output(UInt(64.W))
}

class ToDecode extends Bundle {
  val regIndex  = Input(UInt(5.W))
  val dataValid = Input(Bool())
  val data      = Input(UInt(64.W))
  val csrIndex  = Input(Vec(3, UInt(12.W)))
}

class InstructionDecodeUnit extends Module {
  val regIO          = IO(Flipped(new RegReadIO()))
  val csrIO          = IO(Flipped(new ControlRegisterFileDataIO()))
  val decodeIn       = IO(Flipped(Decoupled(new DecodeIn())))
  val decodeOut      = IO(Decoupled(new ExeIn()))
  val decodeBack     = IO(new DecodeBack())
  val fromExe        = IO(new ToDecode())
  val fromMemu       = IO(new ToDecode())
  val fromWbu        = IO(new ToDecode())
  val fromSelf       = Wire(new ToDecode())
  val controlDecoder = Module(new InstContorlDecoder)

  val decodeInReg = RegInit(DecodeIn.default)

  val willTakeBranch = Wire(Bool())
  val shouldWait     = Wire(Bool())

  val dataValid = RegInit(false.B)

  decodeInReg := Mux(decodeIn.fire, decodeIn.bits, decodeInReg)

  // decodeout.control
  controlDecoder.input := decodeInReg.inst

  // decodeout.data
  val inst = decodeInReg.inst
  val rs1  = inst(19, 15)
  val rs2  = inst(24, 20)
  val rd   = inst(11, 7)
  val immI = Utils.signExtend(inst(31, 20), 12)
  val immS = Utils.signExtend(Cat(inst(31, 25), inst(11, 7)), 12)
  val immU = Utils.signExtend(inst(31, 12), 20) << 12
  val immB = Cat(Utils.signExtend(inst(31), 1), inst(7), inst(30, 25), inst(11, 8), 0.U(1.W))
  val immJ = Cat(Utils.signExtend(inst(31), 1), inst(19, 12), inst(20), inst(30, 21), 0.U(1.W));
  val imm = MuxLookup(controlDecoder.output.insttype, immI)(
    EnumSeq(
      InstType.I -> immI,
      InstType.S -> immS,
      InstType.U -> immU,
      InstType.B -> immB,
      InstType.J -> immJ
    )
  )
  decodeOut.bits.data.imm   := imm
  decodeOut.bits.data.src1  := rs1
  decodeOut.bits.data.src2  := rs2
  decodeOut.bits.data.dst   := rd
  decodeOut.bits.data.wdata := decodeInReg.pc + 4.U

  decodeOut.valid        := dataValid && !shouldWait
  decodeOut.bits.data.pc := decodeInReg.pc
  decodeOut.bits.control := controlDecoder.output

  decodeIn.ready := !dataValid || decodeOut.fire

  dataValid := dataValid ^ decodeOut.fire ^ decodeIn.fire
  // regIO
  regIO.raddr0 := rs1
  regIO.raddr1 := rs2
  val src1RawData = MuxCase(
    regIO.out0,
    Seq(fromExe, fromMemu, fromWbu, fromSelf).map(bundle =>
      (bundle.regIndex === rs1 && bundle.dataValid) -> bundle.data
    )
  )
  val src2RawData = MuxCase(
    regIO.out1,
    Seq(fromExe, fromMemu, fromWbu, fromSelf).map(bundle =>
      (bundle.regIndex === rs2 && bundle.dataValid) -> bundle.data
    )
  )
  val src1Data = Mux(
    controlDecoder.output.srccast1,
    Utils.cast(src1RawData, 32, 64),
    regIO.out0
  )
  val src2Data = Mux(
    controlDecoder.output.srccast2,
    Utils.cast(src2RawData, 32, 64),
    regIO.out1
  )
  decodeOut.bits.data.src1Data := src1Data
  decodeOut.bits.data.src2Data := src2Data

  fromSelf.regIndex  := rd
  fromSelf.dataValid := controlDecoder.output.regwritemux === RegWriteMux.snpc.asUInt
  fromSelf.data      := decodeInReg.pc + 4.U

  // RAW check
  val regVec = VecInit(Seq(fromExe, fromMemu, fromWbu).map(bundle => Mux(bundle.dataValid, 0.U, bundle.regIndex)))
  val csrVec =
    Seq(fromExe, fromMemu, fromWbu).map(bundle => bundle.csrIndex).reduce((prev, s) => VecInit(prev ++ s))
  shouldWait := dataValid && ((rs1 =/= 0.U && regVec.contains(rs1)) ||
    (rs2 =/= 0.U && regVec.contains(rs2)) ||
    (willTakeBranch && controlDecoder.output.pcsrc === PcSrc.csr.asUInt &&
      csrVec.contains(
        ControlRegisters.behaveReadDependency(controlDecoder.output.csrbehave)
      )))

  // branch check
  willTakeBranch := dataValid && MuxLookup(controlDecoder.output.pcaddrsrc, false.B)(
    EnumSeq(
      PCAddrSrc.aluzero -> (src1Data === src2Data),
      PCAddrSrc.alunotneg -> (src1Data.asSInt >= src2Data.asSInt),
      PCAddrSrc.alucarryorzero -> (src1Data >= src2Data),
      PCAddrSrc.aluneg -> (src1Data.asSInt < src2Data.asSInt),
      PCAddrSrc.alunotcarryandnotzero -> (src1Data < src2Data),
      PCAddrSrc.alunotzero -> (src1Data =/= src2Data),
      PCAddrSrc.one -> true.B
    )
  )
  val branchPc = MuxLookup(controlDecoder.output.pcsrc, 0.U)(
    EnumSeq(
      PcSrc.pc -> (decodeInReg.pc + imm),
      PcSrc.src1 -> (src1Data + imm),
      PcSrc.csr -> csrIO.output
    )
  )

  decodeBack.valid          := dataValid && !shouldWait
  decodeBack.willTakeBranch := willTakeBranch
  decodeBack.branchPc       := branchPc

  decodeOut.bits.data.dnpc     := Mux(shouldWait, decodeInReg.pc, Mux(willTakeBranch, branchPc, decodeInReg.pc + 4.U))
  decodeOut.bits.toDecodeValid := fromSelf.dataValid

  csrIO.csrBehave := controlDecoder.output.csrbehave
  // debug
  decodeOut.bits.debug.pc   := decodeInReg.debug.pc
  decodeOut.bits.debug.inst := inst

}
