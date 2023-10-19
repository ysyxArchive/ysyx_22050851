import chisel3.experimental._
import chisel3._
import chisel3.util._
import decode._
import os.read
import execute._
import utils._

class ExeDataIn extends Bundle {
  val src1 = Output(UInt(5.W))
  val src2 = Output(UInt(5.W))
  val dst  = Output(UInt(5.W))
  val imm  = Output(UInt(64.W))
  val pc   = Output(UInt(64.W))

}

class ExeIn extends Bundle {
  val debug   = Output(new DebugInfo)
  val data    = Output(new ExeDataIn);
  val control = Output(new ExeControlIn);
}

class InstructionExecuteUnit extends Module {
  val exeIn  = IO(Flipped(Decoupled(new ExeIn())))
  val exeOut = IO(Decoupled(new MemRWIn()))
  val regIO  = IO(Flipped(new RegReadIO()))
  val csrIn  = IO(Input(UInt(64.W)))
  // val csrControl = IO(Flipped(new CSRFileControl()))

  val exeInReg = Reg(new ExeIn())

  val alu = Module(new ALU)

  val shouldWaitALU = !alu.io.out.bits.isImmidiate

  val idle :: waitDecode :: waitALU :: waitSend :: other = Enum(10)

  val exeFSM = new FSM(
    idle,
    List(
      (waitDecode, exeIn.fire && shouldWaitALU, waitALU),
      (waitDecode, exeIn.fire, waitSend),
      (waitALU, alu.io.out.fire, waitSend),
      (waitSend, exeOut.fire, idle),
      (idle, exeOut.ready, waitDecode)
    )
  )

  exeInReg := Mux(exeIn.fire, exeIn.bits, exeInReg)
  // regIO
  val src1 = Wire(UInt(64.W))
  val src2 = Wire(UInt(64.W))
  regIO.raddr0 := exeInReg.data.src1
  regIO.raddr1 := exeInReg.data.src2
  val snpc = regIO.pc + 4.U
  val pcBranch = MuxLookup(exeInReg.control.pcaddrsrc, false.B)(
    EnumSeq(
      PCAddrSrc.aluzero -> alu.io.out.bits.signals.isZero,
      PCAddrSrc.aluneg -> alu.io.out.bits.signals.isNegative,
      PCAddrSrc.alunotneg -> !alu.io.out.bits.signals.isNegative,
      PCAddrSrc.alunotzero -> !alu.io.out.bits.signals.isZero,
      PCAddrSrc.alunotcarryandnotzero -> (!alu.io.out.bits.signals.isCarry && !alu.io.out.bits.signals.isZero),
      PCAddrSrc.alucarryorzero -> (alu.io.out.bits.signals.isCarry || alu.io.out.bits.signals.isZero),
      PCAddrSrc.zero -> false.B,
      PCAddrSrc.one -> true.B
    )
  )
  // val csrInReg = RegInit(csrIn)
  // csrInReg := Mux(exeFSM.willChangeTo(waitPC), csrIn, csrInReg)
  val dnpcAddSrcReg = RegNext(
    MuxLookup(exeInReg.control.pcsrc, regIO.pc)(
      EnumSeq(
        PcSrc.pc -> regIO.pc,
        PcSrc.src1 -> src1
      )
    )
  )

  src1 :=
    Mux(
      exeInReg.control.srccast1,
      Utils.cast(regIO.out0, 32, 64),
      regIO.out0
    )
  src2 :=
    Mux(
      exeInReg.control.srccast2,
      Utils.cast(regIO.out1, 32, 64),
      regIO.out1
    )

  // alu
  alu.io.in.bits.inA := MuxLookup(exeInReg.control.alumux1, 0.U)(
    EnumSeq(
      AluMux1.pc -> regIO.pc,
      AluMux1.src1 -> src1,
      AluMux1.zero -> 0.U
    )
  )
  alu.io.in.bits.inB := MuxLookup(exeInReg.control.alumux2, 0.U)(
    EnumSeq(
      AluMux2.imm -> exeInReg.data.imm,
      AluMux2.src2 -> src2
    )
  )
  val res = AluMode.safe(exeInReg.control.alumode)
  alu.io.in.bits.opType := res._1
  alu.io.out.ready      := alu.io.out.bits.isImmidiate || exeFSM.is(waitALU)
  alu.io.in.valid       := exeIn.fire
  // csr
  // csrControl.csrBehave  := Mux(exeFSM.willChangeTo(waitPC), exeInReg.control.csrbehave, CsrBehave.no.asUInt)
  // csrControl.csrSetmode := Mux(exeFSM.willChangeTo(waitPC), exeInReg.control.csrsetmode, CsrSetMode.origin.asUInt)
  // csrControl.csrSource  := exeInReg.control.csrsource

  // blackBoxHalt
  val blackBox = Module(new BlackBoxHalt);
  blackBox.io.halt     := exeInReg.control.goodtrap
  blackBox.io.bad_halt := exeInReg.control.badtrap || res._2 === false.B

  exeIn.ready := exeFSM.is(idle)

  exeOut.valid             := exeFSM.is(waitSend)
  exeOut.bits.control      := exeInReg.control
  exeOut.bits.data.alu     := alu.io.out.bits.out
  exeOut.bits.data.src1    := exeInReg.data.src1
  exeOut.bits.data.src2    := exeInReg.data.src2
  exeOut.bits.data.dst     := exeInReg.data.dst
  exeOut.bits.data.signals := alu.io.out.bits.signals
  exeOut.bits.data.pc      := exeInReg.data.pc

  exeOut.bits.debug := exeInReg.debug
}
