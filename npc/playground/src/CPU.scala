import chisel3._
import chisel3.util.Enum
import chisel3.util.Decoupled
import decode._

class CPU extends Module {
  val enableDebug = IO(Input(Bool()))

  val mem         = Module(new MemInterface)
  val regs        = Module(new RegisterFile)
  val csrregs     = Module(new ControlRegisterFile)
  val blackBoxOut = Module(new BlackBoxRegs)

  val ifu     = Module(new InstructionFetchUnit)
  val decoder = Module(new InstructionDecodeUnit)
  val exe     = Module(new InstructionExecuteUnit)
  val memu    = Module(new MemRWUnit())
  val wbu     = Module(new WriteBackUnit())

  val arbiter = Module(new AxiLiteArbiter(2))
  val iCache  = Module(new Cache(name = "icache"))
  val dCache  = Module(new Cache(name = "dcache"))

  ifu.iCacheIO <> iCache.io
  ifu.fetchOut <> decoder.decodeIn
  decoder.decodeOut <> exe.exeIn
  exe.exeOut <> memu.memIn
  memu.memOut <> wbu.wbIn

  iCache.axiIO <> arbiter.slaveIO(0)
  decoder.regIO := regs.readIO

  exe.csrIn := csrregs.io.output
  exe.regIO <> regs.readIO

  dCache.axiIO <> arbiter.slaveIO(1)
  mem.axiS <> arbiter.masterIO

  csrregs.io.data := decoder.decodeOut.bits.data

  wbu.regIO <> regs.writeIO

  blackBoxOut.io.pc      := regs.debugPCOut;
  blackBoxOut.io.regs    := regs.debugOut;
  blackBoxOut.io.csrregs := csrregs.debugOut;

  iCache.enableDebug := enableDebug
  dCache.enableDebug := enableDebug
}
