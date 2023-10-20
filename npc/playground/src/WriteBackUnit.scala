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
}

class WBIn extends Bundle {
  val debug   = Output(new DebugInfo)
  val data    = Output(new WBDataIn);
  val control = Output(new ExeControlIn);
}

class WriteBackUnit extends Module {
  val wbIn = IO(Flipped(Decoupled(new WBIn())))
  // val memIO      = IO(Flipped(new CacheIO(64, 64)))
  val regWriteIO = IO(Flipped(new RegWriteIO()))
  val regReadIO  = IO(Input(new RegReadIO()))
  val csrIn      = IO(Input(UInt(64.W)))
  val csrControl = IO(Flipped(new CSRFileControl()))

  val wbInReg = Reg(new WBIn())

  val idle :: busy :: other = Enum(3)

  val wbFSM = new FSM(
    idle,
    List(
      (idle, wbIn.fire, busy),
      (busy, true.B, idle)
    )
  )

  wbInReg := Mux(wbIn.fire, wbIn.bits, wbInReg)

  // regWriteIO
  regWriteIO.waddr := Mux(wbFSM.is(busy) && wbInReg.control.regwrite, wbInReg.data.dst, 0.U)
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
  val csrInReg = RegInit(csrIn)
  csrInReg := Mux(wbFSM.willChangeTo(busy), csrIn, csrInReg)
  val dnpcAddSrcReg =
    MuxLookup(wbInReg.control.pcsrc, wbInReg.data.pc)(
      EnumSeq(
        PcSrc.pc -> wbInReg.data.pc,
        PcSrc.src1 -> wbInReg.data.src1
      )
    )

  val dnpcAlter = MuxLookup(wbInReg.control.pccsr, dnpcAddSrcReg)(
    EnumSeq(
      PcCsr.origin -> (dnpcAddSrcReg + wbInReg.data.imm),
      PcCsr.csr -> csrInReg
    )
  )
  regWriteIO.dnpc := Mux(wbFSM.is(busy), Mux(pcBranch.asBool, dnpcAlter, snpc), regReadIO.pc)
  val regwdata = MuxLookup(wbInReg.control.regwritemux, wbInReg.data.alu)(
    EnumSeq(
      RegWriteMux.alu -> wbInReg.data.alu,
      RegWriteMux.snpc -> snpc,
      RegWriteMux.mem -> wbInReg.data.mem,
      RegWriteMux.aluneg -> Utils.zeroExtend(wbInReg.data.signals.isNegative, 1, 64),
      RegWriteMux.alunotcarryandnotzero -> Utils
        .zeroExtend(!wbInReg.data.signals.isCarry && !wbInReg.data.signals.isZero, 1, 64),
      RegWriteMux.csr -> csrIn
    )
  )
  regWriteIO.wdata := Mux(wbInReg.control.regwsext, Utils.signExtend(regwdata.asUInt, 32), regwdata)
  // csr
  csrControl.csrBehave  := Mux(wbFSM.willChangeTo(busy), wbInReg.control.csrbehave, CsrBehave.no.asUInt)
  csrControl.csrSetmode := Mux(wbFSM.willChangeTo(busy), wbInReg.control.csrsetmode, CsrSetMode.origin.asUInt)
  csrControl.csrSource  := wbInReg.control.csrsource

  // blackBoxHalt
  val blackBox = Module(new BlackBoxHalt);
  blackBox.io.halt     := wbFSM.willChangeTo(idle) && wbInReg.control.goodtrap
  blackBox.io.bad_halt := wbFSM.willChangeTo(idle) && wbInReg.control.badtrap

  wbIn.ready := wbFSM.is(idle)
}
