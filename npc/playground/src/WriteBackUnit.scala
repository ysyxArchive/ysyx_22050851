import chisel3.experimental._
import chisel3._
import chisel3.util._
import decode._
import os.read
import execute._
import utils._

class WBDataIn extends Bundle {
  val src1     = Output(UInt(5.W))
  val src2     = Output(UInt(5.W))
  val src1Data = Output(UInt(64.W))
  val dst      = Output(UInt(5.W))
  val imm      = Output(UInt(64.W))
  val alu      = Output(UInt(64.W))
  val mem      = Output(UInt(64.W))
  val signals  = new SignalIO()
  val pc       = Output(UInt(64.W))
  val dnpc     = Output(UInt(64.W))
}

class WBIn extends Bundle {
  val debug   = Output(new DebugInfo)
  val data    = Output(new WBDataIn);
  val control = Output(new ExeControlIn);
  val enable  = Output(Bool())
}

class WriteBackUnit extends Module {
  val wbIn       = IO(Flipped(Decoupled(new WBIn())))
  val regWriteIO = IO(Flipped(new RegWriteIO()))
  val regReadIO  = IO(Input(new RegReadIO()))
  val csrControl = IO(Flipped(new ControlRegisterFileControlIO()))
  val toDecode   = IO(Flipped(new ToDecode()))

  val wbInReg   = Reg(new WBIn())
  val wbInValid = Reg(new Bool())

  wbInValid := wbIn.valid
  wbInReg   := Mux(wbIn.valid, wbIn.bits, wbInReg)

  // regWriteIO
  regWriteIO.waddr := Mux(wbInValid && wbInReg.control.regwrite, wbInReg.data.dst, 0.U)
  val snpc = wbInReg.data.pc + 4.U
  val pcBranch = MuxLookup(wbInReg.control.pcaddrsrc, false.B)(
    EnumSeq(
      PCAddrSrc.aluzero -> wbInReg.data.signals.isZero,
      PCAddrSrc.aluneg -> wbInReg.data.signals.isNegative,
      PCAddrSrc.alunotneg -> !wbInReg.data.signals.isNegative,
      PCAddrSrc.alunotzero -> !wbInReg.data.signals.isZero,
      PCAddrSrc.alunotcarryandnotzero -> (!wbInReg.data.signals.isCarry && !wbInReg.data.signals.isZero),
      PCAddrSrc.alucarryorzero -> (wbInReg.data.signals.isCarry || wbInReg.data.signals.isZero),
      PCAddrSrc.zero -> false.B,
      PCAddrSrc.one -> true.B
    )
  )
  regWriteIO.dnpc := Mux(wbInValid, wbInReg.data.dnpc, regReadIO.pc)
  val regwdata = MuxLookup(wbInReg.control.regwritemux, wbInReg.data.alu)(
    EnumSeq(
      RegWriteMux.alu -> wbInReg.data.alu,
      RegWriteMux.snpc -> snpc,
      RegWriteMux.mem -> wbInReg.data.mem,
      RegWriteMux.aluneg -> Utils.zeroExtend(wbInReg.data.signals.isNegative, 1, 64),
      RegWriteMux.alunotcarryandnotzero -> Utils
        .zeroExtend(!wbInReg.data.signals.isCarry && !wbInReg.data.signals.isZero, 1, 64),
      RegWriteMux.csr -> csrControl.output
    )
  )
  regWriteIO.wdata := Mux(wbInReg.control.regwsext, Utils.signExtend(regwdata.asUInt, 32), regwdata)
  // csr
  csrControl.control.csrBehave  := Mux(wbIn.valid, wbInReg.control.csrbehave, CsrBehave.no.asUInt)
  csrControl.control.csrSetmode := Mux(wbIn.valid, wbInReg.control.csrsetmode, CsrSetMode.origin.asUInt)
  csrControl.control.csrSource  := wbInReg.control.csrsource
  csrControl.data               := wbInReg.data

  // blackBoxHalt
  val blackBox = Module(new BlackBoxHalt);
  blackBox.io.halt     := wbIn.valid && wbInReg.control.goodtrap
  blackBox.io.bad_halt := wbIn.valid && wbInReg.control.badtrap

  wbIn.ready := true.B

  toDecode.regIndex := Mux(wbInValid, wbInReg.data.dst, 0.U)
  toDecode.csrIndex := Mux(
    wbInValid,
    ControlRegisters.behaveDependency(wbInReg.control.csrbehave, wbInReg.control.csrsetmode, wbInReg.data.imm),
    0.U
  )
}
