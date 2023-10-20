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
}

class ExeIn extends Bundle {
  val debug   = Output(new DebugInfo)
  val data    = Output(new ExeDataIn);
  val control = Output(new ExeControlIn);
  val enable  = Output(Bool())
}

class InstructionExecuteUnit extends Module {
  val exeIn    = IO(Flipped(Decoupled(new ExeIn())))
  val exeOut   = IO(Decoupled(new MemRWIn()))
  val csrIn    = IO(Input(UInt(64.W)))
  val toDecode = IO(Output(UInt(5.W)))

  val exeInReg = Reg(new ExeIn())

  val alu = Module(new ALU)

  val shouldWaitALU = !alu.io.out.bits.isImmidiate

  val waitDecode :: waitALU :: waitSend :: other = Enum(10)

  val exeFSM = new FSM(
    waitDecode,
    List(
      (waitDecode, exeIn.fire && shouldWaitALU, waitALU),
      (waitDecode, exeIn.fire, waitSend),
      (waitALU, alu.io.out.fire, waitSend),
      (waitSend, exeOut.fire, waitDecode)
    )
  )

  exeInReg := Mux(exeIn.fire, exeIn.bits, exeInReg)
  exeInReg.control := Mux(
    exeIn.fire,
    Mux(exeIn.bits.enable, exeIn.bits.control, ExeControlIn.default()),
    exeInReg.control
  )

  // alu
  alu.io.in.bits.inA := MuxLookup(exeInReg.control.alumux1, 0.U)(
    EnumSeq(
      AluMux1.pc -> exeInReg.data.pc,
      AluMux1.src1 -> exeInReg.data.src1Data,
      AluMux1.zero -> 0.U
    )
  )
  alu.io.in.bits.inB := MuxLookup(exeInReg.control.alumux2, 0.U)(
    EnumSeq(
      AluMux2.imm -> exeInReg.data.imm,
      AluMux2.src2 -> exeInReg.data.src2Data
    )
  )
  val res = AluMode.safe(exeInReg.control.alumode)
  alu.io.in.bits.opType := res._1
  alu.io.out.ready      := alu.io.out.bits.isImmidiate || exeFSM.is(waitALU)
  alu.io.in.valid       := exeIn.fire

  exeIn.ready := exeFSM.is(waitDecode)

  exeOut.valid              := exeFSM.is(waitSend)
  exeOut.bits.control       := exeInReg.control
  exeOut.bits.data.alu      := alu.io.out.bits.out
  exeOut.bits.data.src1     := exeInReg.data.src1
  exeOut.bits.data.src2     := exeInReg.data.src2
  exeOut.bits.data.dst      := exeInReg.data.dst
  exeOut.bits.data.signals  := alu.io.out.bits.signals
  exeOut.bits.data.pc       := exeInReg.data.pc
  exeOut.bits.data.dnpc     := exeInReg.data.dnpc
  exeOut.bits.data.imm      := exeInReg.data.imm
  exeOut.bits.data.src1Data := exeInReg.data.src1Data
  exeOut.bits.enable        := exeInReg.enable
  
  exeOut.bits.debug         := exeInReg.debug

  toDecode := exeInReg.data.dst

}
