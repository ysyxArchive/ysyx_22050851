import chisel3._
import chisel3.util.Enum
import chisel3.util.Decoupled
import decode._

/**
  * Compute GCD using subtraction method.
  * Subtracts the smaller from the larger until register y is zero.
  * value in register x is then the GCD
  */

class CPU extends Module {
  val pcio = IO(new Bundle {
    val inst = Input(UInt(32.W))
    val pc   = Output(UInt(64.W))
  })
  val regs        = Module(new RegisterFile)
  val csrregs     = Module(new ControlRegisterFile)
  val ifu         = Module(new InstructionFetchUnit)
  val decoder     = Module(new InstructionDecodeUnit)
  val exe         = Module(new InstructionExecuteUnit)
  val mem         = Module(new BlackBoxMem)
  val blackBoxOut = Module(new BlackBoxRegs);


  ifu.instOut <> decoder.instIn
  
  exe.decodeIn := decoder.decodeOut
  exe.regIO <> regs.io
  exe.memIO <> mem.io
  exe.csrIn := csrregs.io.output

  csrregs.io.decodeIn := decoder.decodeOut
  csrregs.io.src1Data := regs.io.out0
  csrregs.regIn       := regs.io

  blackBoxOut.io.pc      := regs.io.pc;
  blackBoxOut.io.regs    := regs.debugOut;
  blackBoxOut.io.csrregs := csrregs.debugOut;
}
