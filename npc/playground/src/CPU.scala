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

  val regs    = Module(new RegisterFile);
  val decoder = Module(new InstructionDecodeUnit)
  val exe     = Module(new InstructionExecuteUnit)
  val mem     = Module(new BlackBoxMem)

  pcio.pc := regs.io.pc

  decoder.io.inst   := pcio.inst
  decoder.io.enable := true.B

  exe.in <> decoder.output
  exe.regIO <> regs.io
  exe.memIO <> mem.io

}
