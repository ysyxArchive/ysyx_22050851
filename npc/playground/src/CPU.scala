import chisel3._
import chisel3.util.Enum
import chisel3.util.Decoupled
import decode._

class CPU extends Module {
  val enableDebug = IO(Input(Bool()))

  val mem         = Module(new MemInterface)
  val regs        = Module(new RegisterFile)
  val csrregs     = Module(new ControlRegisterFile)
  val ifu         = Module(new InstructionFetchUnit)
  val decoder     = Module(new InstructionDecodeUnit)
  val exe         = Module(new InstructionExecuteUnit)
  val blackBoxOut = Module(new BlackBoxRegs)
  val arbiter     = Module(new AxiLiteArbiter(2))

  val iCache = Module(new Cache(name = "icache"))
  val dCache = Module(new Cache(name = "dcache"))

  decoder.iCacheIO <> iCache.io
  iCache.axiIO <> arbiter.slaveIO(0)
  decoder.regIO := regs.io

  exe.decodeIn <> decoder.decodeOut
  exe.regIO <> regs.io
  dCache.axiIO <> arbiter.slaveIO(1)
  exe.memIO <> dCache.io
  exe.csrIn := csrregs.io.output

  mem.axiS <> arbiter.masterIO

  csrregs.io.data     := decoder.decodeOut.bits.data
  csrregs.io.src1Data := regs.io.out0
  csrregs.io.control  := exe.csrControl
  csrregs.regIn       := regs.io

  blackBoxOut.io.pc      := regs.debugPCOut;
  blackBoxOut.io.regs    := regs.debugOut;
  blackBoxOut.io.csrregs := csrregs.debugOut;

  iCache.enableDebug := enableDebug
  dCache.enableDebug := enableDebug
}
