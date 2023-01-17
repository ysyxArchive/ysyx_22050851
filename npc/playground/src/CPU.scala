import chisel3._
import chisel3.util.Enum
import chisel3.util.Decoupled

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
  val out = IO(Decoupled(Operation()))
  val debugout = IO(new Bundle {
    val regs   = Output(Vec(32, UInt(64.W)))
    val debugp = Output(UInt(3.W))

  })

  val regs = Module(new RegisterFile);
  debugout.regs := regs.debugout.regs
  pcio.pc       := regs.io.pc
//   regs.io := DontCare

//   val pc = RegInit("h80000000".asUInt(64.W))

  val decoder = Module(new InstructionDecodeUnit)
  decoder.io.inst   := pcio.inst
  decoder.io.enable := true.B
  debugout          := decoder.debugp
  val exe = Module(new InstructionExecuteUnit)

  out <> decoder.output
  exe.in <> decoder.output
  exe.regIO <> regs.io

}
