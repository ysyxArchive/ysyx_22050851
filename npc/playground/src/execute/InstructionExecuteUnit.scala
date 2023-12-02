import chisel3.experimental._
import chisel3._
import chisel3.util._
import decode._
import os.read
import execute._
import utils._

class ExeDataIn extends Bundle {
  val src1     = Output(UInt(5.W))
  val src1Data = Output(UInt(64.W))
  val src2     = Output(UInt(5.W))
  val src2Data = Output(UInt(64.W))
  val dst      = Output(UInt(5.W))
  val imm      = Output(UInt(64.W))
  val pc       = Output(UInt(64.W))
  val dnpc     = Output(UInt(64.W))
  val wdata    = Output(UInt(64.W))
}

class ExeIn extends Bundle {
  val debug         = Output(new DebugInfo)
  val data          = Output(new ExeDataIn);
  val control       = Output(new ExeControlIn);
  val toDecodeValid = Output(Bool())
}

class InstructionExecuteUnit extends Module {
  val exeIn    = IO(Flipped(Decoupled(new ExeIn())))
  val exeOut   = IO(Decoupled(new MemRWIn()))
  val toDecode = IO(Flipped(new ForwardData()))
  val fromMemu = IO(new ForwardData())
  val fromWbu  = IO(new ForwardData())

  val alu = Module(new ALU)

  val exeInReg = Reg(new ExeIn())
  exeInReg := Mux(exeIn.fire, exeIn.bits, exeInReg)

  val dataValid = RegInit(false.B)
  dataValid := dataValid ^ exeIn.fire ^ exeOut.fire

  val mulOps = VecInit(Seq(AluMode.mul, AluMode.mulw, AluMode.mulh, AluMode.mulhsu, AluMode.mulhu).map(t => t.asUInt))
  val divOps = VecInit(Seq(AluMode.div, AluMode.divu, AluMode.divw, AluMode.divuw).map(t => t.asUInt))
  val remOps = VecInit(Seq(AluMode.rem, AluMode.remu, AluMode.remw, AluMode.remuw).map(t => t.asUInt))
  val ops    = VecInit(mulOps ++ divOps ++ remOps)

  val shouldWaitALU = ops.contains(exeInReg.control.alumode.asUInt)

  val regVec = VecInit(Seq(fromMemu, fromWbu).map(bundle => Mux(bundle.dataValid, 0.U, bundle.regIndex)))
  val rs1    = exeInReg.data.src1
  val rs2    = exeInReg.data.src2
  val src1RawData = MuxCase(
    exeInReg.data.src1Data,
    Seq(fromMemu, fromWbu).map(bundle => (bundle.regIndex === rs1 && rs1.orR && bundle.dataValid) -> bundle.data)
  )
  val src2RawData = MuxCase(
    exeInReg.data.src2Data,
    Seq(fromMemu, fromWbu).map(bundle => (bundle.regIndex === rs2 && rs2.orR && bundle.dataValid) -> bundle.data)
  )
  val src1Data = Mux(
    exeInReg.control.srccast1,
    Utils.cast(src1RawData, 32, 64),
    src1RawData
  )
  val src2Data = Mux(
    exeInReg.control.srccast2,
    Utils.cast(src2RawData, 32, 64),
    src2RawData
  )

  val shouldWait = dataValid &&
    ((rs1 =/= 0.U && regVec.contains(rs1) && exeInReg.control.alumux1 === AluMux1.src1.asUInt) ||
      (rs2 =/= 0.U && regVec.contains(rs2) && exeInReg.control.alumux2 === AluMux2.src2.asUInt))

  // alu
  alu.io.in.bits.inA := MuxLookup(exeInReg.control.alumux1, 0.U)(
    EnumSeq(
      AluMux1.pc -> exeInReg.data.pc,
      AluMux1.src1 -> src1Data,
      AluMux1.zero -> 0.U
    )
  )
  alu.io.in.bits.inB := MuxLookup(exeInReg.control.alumux2, 0.U)(
    EnumSeq(
      AluMux2.imm -> exeInReg.data.imm,
      AluMux2.src2 -> src2Data
    )
  )
  val wdata = MuxLookup(exeInReg.control.regwritemux, 0.U)(
    EnumSeq(
      RegWriteMux.alu -> alu.io.out.bits.out,
      RegWriteMux.aluneg -> Utils.zeroExtend(alu.io.out.bits.signals.isNegative, 1, 64), // from EXU
      RegWriteMux.alunotcarryandnotzero -> Utils
        .zeroExtend(!alu.io.out.bits.signals.isCarry && !alu.io.out.bits.signals.isZero, 1, 64) // from EXU
    )
  )
  val wdataExtended = Mux(exeInReg.control.regwsext, Utils.signExtend(wdata, 32), wdata)

  val res = AluMode.safe(exeInReg.control.alumode)
  alu.io.in.bits.opType := res._1
  alu.io.out.ready      := true.B
  alu.io.in.valid       := dataValid && !shouldWait

  exeIn.ready := !dataValid || exeOut.fire

  exeOut.valid              := (dataValid && !shouldWaitALU && !shouldWait) || (dataValid && alu.io.out.fire)
  exeOut.bits.control       := exeInReg.control
  exeOut.bits.data.alu      := alu.io.out.bits.out
  exeOut.bits.data.src1     := exeInReg.data.src1
  exeOut.bits.data.src2     := exeInReg.data.src2
  exeOut.bits.data.dst      := exeInReg.data.dst
  exeOut.bits.data.signals  := alu.io.out.bits.signals
  exeOut.bits.data.pc       := exeInReg.data.pc
  exeOut.bits.data.dnpc     := exeInReg.data.dnpc
  exeOut.bits.data.imm      := exeInReg.data.imm
  exeOut.bits.data.src1Data := src1Data
  exeOut.bits.data.src2Data := src2Data
  exeOut.bits.data.wdata    := Mux(exeInReg.toDecodeValid, exeInReg.data.wdata, wdataExtended)
  exeOut.bits.toDecodeValid := toDecode.dataValid

  exeOut.bits.debug := exeInReg.debug

  toDecode.regIndex := Mux(dataValid, exeInReg.data.dst, 0.U)
  toDecode.dataValid := dataValid && (exeInReg.toDecodeValid || ((!shouldWaitALU || alu.io.out.fire) && VecInit(
    RegWriteMux.alu.asUInt,
    RegWriteMux.aluneg.asUInt,
    RegWriteMux.alunotcarryandnotzero.asUInt
  ).contains(exeInReg.control.regwritemux)))
  toDecode.data := Mux(exeInReg.toDecodeValid, exeInReg.data.wdata, wdataExtended)
  toDecode.csrIndex := Mux(
    dataValid,
    ControlRegisters.behaveDependency(exeInReg.control.csrbehave, exeInReg.control.csrsetmode, exeInReg.data.imm),
    VecInit.fill(3)(0.U(12.W))
  )
}
