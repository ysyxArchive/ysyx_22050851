import chisel3._
import chisel3.util.Enum
import chisel3.util.Decoupled
import decode._

class CPU(isDebug: Boolean) extends Module {
  val enableDebug = IO(Input(Bool()))
  val isHalt      = IO(Bool())
  val isGoodHalt  = IO(Bool())

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
  // val iCache  = Module(new Cache(name = "icache"))
  // val dCache  = Module(new Cache(name = "dcache"))
  val iCache = Module(new Cache(name = "icache", wayCnt = 2, groupSize = 2, cellByte = 16, isDebug = isDebug))
  val dCache = Module(new Cache(name = "dcache", wayCnt = 2, groupSize = 2, cellByte = 16, isDebug = isDebug))
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
  decoder.csrIO <> csrregs.dataIO
  decoder.fromExe  := exe.toDecode
  decoder.fromMemu := memu.toDecode
  decoder.fromWbu  := wbu.toDecode
  exe.fromMemu     := memu.toDecode
  exe.fromWbu      := wbu.toDecode
  memu.fromWbu     := wbu.toDecode

  memu.memIO <> dCache.io

  wbu.regWriteIO <> regs.writeIO
  wbu.regReadIO := regs.readIO
  wbu.csrControl <> csrregs.controlIO

  blackBoxOut.io.pc      := regs.debugPCOut;
  blackBoxOut.io.regs    := regs.debugOut;
  blackBoxOut.io.csrregs := csrregs.debugOut;

  isHalt     := wbu.isHalt
  isGoodHalt := wbu.isGoodHalt

  if (isDebug) {
    val blackBoxPip = Module(new BlackBoxPip)
    blackBoxPip.io.clock   := clock
    blackBoxPip.io.ifHalt  := !ifu.fetchOut.valid
    blackBoxPip.io.idHalt  := !decoder.decodeIn.ready
    blackBoxPip.io.exHalt  := !exe.exeIn.ready
    blackBoxPip.io.memHalt := !memu.memIn.ready
    blackBoxPip.io.wbHalt  := !wbu.wbIn.ready
  }
}
