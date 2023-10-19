import chisel3.experimental._
import chisel3._
import chisel3.util._
import decode._
import os.read
import execute._
import utils._

class WBDataIn extends Bundle {
  val src1    = Output(UInt(5.W))
  val src2    = Output(UInt(5.W))
  val dst     = Output(UInt(5.W))
  val imm     = Output(UInt(64.W))
  val alu     = Output(UInt(64.W))
  val mem     = Output(UInt(64.W))
  val signals = new SignalIO()
  val pc      = Input(UInt(64.W))
}

class WBIn extends Bundle {
  val debug   = Output(new DebugInfo)
  val data    = Output(new WBDataIn);
  val control = Output(new ExeControlIn);
}

class WriteBackUnit extends Module {
  val wbIn = IO(Flipped(Decoupled(new WBIn())))
  // val memIO      = IO(Flipped(new CacheIO(64, 64)))
  val regIO      = IO(Flipped(new RegWriteIO()))
  val csrIn      = IO(Input(UInt(64.W)))
  val csrControl = IO(Flipped(new CSRFileControl()))

  val wbInReg = Reg(new WBIn())

  val idle :: waitPC :: other = Enum(3)

  val wbFSM = new FSM(
    idle,
    List(
      (idle, wbIn.fire, waitPC),
      (waitPC, true.B, idle)
    )
  )

  wbInReg := Mux(wbIn.fire, wbIn.bits, wbInReg)

  // regIO
  // val src1 = Wire(UInt(64.W))
  // val src2 = Wire(UInt(64.W))
  // regIO.raddr0 := wbInReg.data.src1
  // regIO.raddr1 := wbInReg.data.src2
  regIO.waddr := Mux(wbInReg.control.regwrite && wbIn.fire, wbInReg.data.dst, 0.U)
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
  csrInReg := Mux(wbFSM.willChangeTo(waitPC), csrIn, csrInReg)
  val dnpcAddSrcReg = RegNext(
    MuxLookup(wbInReg.control.pcsrc, wbInReg.data.pc)(
      EnumSeq(
        PcSrc.pc -> wbInReg.data.pc,
        PcSrc.src1 -> wbInReg.data.src1
      )
    )
  )
  val dnpcAlter = MuxLookup(wbInReg.control.pccsr, dnpcAddSrcReg)(
    EnumSeq(
      PcCsr.origin -> (dnpcAddSrcReg + wbInReg.data.imm),
      PcCsr.csr -> csrInReg
    )
  )
  regIO.dnpc := Mux(wbFSM.is(waitPC), Mux(pcBranch.asBool, dnpcAlter, snpc), wbInReg.data.pc)
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
  regIO.wdata := Mux(wbInReg.control.regwsext, Utils.signExtend(regwdata.asUInt, 32), regwdata)
  // csr
  csrControl.csrBehave  := Mux(wbFSM.willChangeTo(waitPC), wbInReg.control.csrbehave, CsrBehave.no.asUInt)
  csrControl.csrSetmode := Mux(wbFSM.willChangeTo(waitPC), wbInReg.control.csrsetmode, CsrSetMode.origin.asUInt)
  csrControl.csrSource  := wbInReg.control.csrsource

  wbIn.ready := wbFSM.is(idle)
}
