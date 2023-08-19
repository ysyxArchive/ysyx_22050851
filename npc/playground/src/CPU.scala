import chisel3._
import chisel3.util.Enum
import chisel3.util.Decoupled
import decode._

class CPU extends Module {
  val mem     = Module(new MemInterface)
  val mem2    = Module(new MemInterface)
  val regs    = Module(new RegisterFile)
  val csrregs = Module(new ControlRegisterFile)
  // val ifu         = Module(new InstructionFetchUnit)
  val decoder     = Module(new InstructionDecodeUnit)
  val exe         = Module(new InstructionExecuteUnit)
  val blackBoxOut = Module(new BlackBoxRegs)

  decoder.memAxiM <> mem2.axiS
  decoder.regIO := regs.io

  exe.decodeIn <> decoder.decodeOut
  exe.regIO <> regs.io
  exe.memAxiM <> mem.axiS
  exe.csrIn := csrregs.io.output

  csrregs.io.decodeIn := decoder.decodeOut
  csrregs.io.src1Data := regs.io.out0
  csrregs.regIn       := regs.io

  blackBoxOut.io.pc      := regs.debugPCOut;
  blackBoxOut.io.regs    := regs.debugOut;
  blackBoxOut.io.csrregs := csrregs.debugOut;
}
