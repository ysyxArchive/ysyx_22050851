import chisel3._
import chisel3.util.Enum

/**
 * Compute GCD using subtraction method.
 * Subtracts the smaller from the larger until register y is zero.
 * value in register x is then the GCD
 */


class CPU extends Module {
  val io = IO(new Bundle {

    val fetok = Input(Bool())
  })

  val regs = Module(new RegisterFile);
  val pc = RegInit("h80000000".asUInt(64.W))

  val state_fetch :: state_decode :: state_execute :: state_idle = Enum(3)
  val cpuState = RegInit(state_fetch)

  val decoder = Module(new InstructionDecodeUnit)
  val exe = Module(new InstructionExecuteUnit)


}