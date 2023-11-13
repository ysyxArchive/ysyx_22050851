import chisel3._
import chisel3.util.Enum
import chisel3.util.Decoupled
import decode._

class CPU extends Module {
  val enableDebug = IO(Input(Bool()))

  val mem         = Module(new MemBurstInterface)
  val regs        = Module(new RegisterFile)
  val csrregs     = Module(new ControlRegisterFile)
  val blackBoxOut = Module(new BlackBoxRegs)

  val ifu     = Module(new InstructionFetchUnit)
  val decoder = Module(new InstructionDecodeUnit)
  val exe     = Module(new InstructionExecuteUnit)
  val memu    = Module(new MemRWUnit())
  val wbu     = Module(new WriteBackUnit())

  val arbiter = Module(new BurstLiteArbiter(2))
  val iCache  = Module(new Cache(name = "icache"))
  val dCache  = Module(new Cache(name = "dcache"))
  ifu.fetchOut <> decoder.decodeIn
  decoder.decodeOut <> exe.exeIn
  exe.exeOut <> memu.memIn
  memu.memOut <> wbu.wbIn

  ifu.fromDecode <> decoder.decodeBack

  iCache.axiIO <> arbiter.masterIO(1)
  dCache.axiIO <> arbiter.masterIO(0)
  mem.axiS <> arbiter.slaveIO

  ifu.iCacheIO <> iCache.io
  ifu.regIO := regs.readIO

  decoder.regIO <> regs.readIO
  decoder.fromExe  := exe.toDecode
  decoder.fromMemu := memu.toDecode
  decoder.fromWbu  := wbu.toDecode

  exe.csrIn := csrregs.io.output

  memu.memIO <> dCache.io

  wbu.csrIn := csrregs.io.output
  wbu.regWriteIO <> regs.writeIO
  wbu.regReadIO := regs.readIO

  csrregs.io.data := wbu.wbInReg
  csrregs.io.control <> wbu.csrControl

  blackBoxOut.io.pc      := regs.debugPCOut;
  blackBoxOut.io.regs    := regs.debugOut;
  blackBoxOut.io.csrregs := csrregs.debugOut;

  iCache.enableDebug := enableDebug
  dCache.enableDebug := enableDebug
}
