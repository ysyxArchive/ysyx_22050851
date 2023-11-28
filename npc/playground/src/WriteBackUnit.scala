import chisel3.experimental._
import chisel3._
import chisel3.util._
import decode._
import os.read
import execute._
import utils._

class WBDataIn extends Bundle {
  val src1     = Output(UInt(5.W))
  val src1Data = Output(UInt(64.W))
  val dst      = Output(UInt(5.W))
  val imm      = Output(UInt(64.W))
  val pc       = Output(UInt(64.W))
  val dnpc     = Output(UInt(64.W))
  val wdata    = Output(UInt(64.W))
}

class WBIn extends Bundle {
  val debug         = Output(new DebugInfo)
  val data          = Output(new WBDataIn);
  val control       = Output(new ExeControlIn);
  val toDecodeValid = Output(Bool())
}

class WriteBackUnit extends Module {
  val wbIn       = IO(Flipped(Decoupled(new WBIn())))
  val regWriteIO = IO(Flipped(new RegWriteIO()))
  val regReadIO  = IO(Input(new RegReadIO()))
  val csrControl = IO(Flipped(new ControlRegisterFileControlIO()))
  val toDecode   = IO(Flipped(new ForwardData()))

  val wbInReg   = Reg(new WBIn())
  val wbInValid = Reg(new Bool())

  wbInValid := wbIn.valid
  wbInReg   := Mux(wbIn.valid, wbIn.bits, wbInReg)

  // regWriteIO
  regWriteIO.waddr := Mux(wbInValid && wbInReg.control.regwritemux =/= RegWriteMux.no.asUInt, wbInReg.data.dst, 0.U)
  regWriteIO.dnpc  := Mux(wbInValid, wbInReg.data.dnpc, regReadIO.pc)

  regWriteIO.wdata := Mux(wbInReg.control.regwritemux === RegWriteMux.csr.asUInt, csrControl.output, wbInReg.data.wdata)
  // csr
  csrControl.control.csrBehave  := Mux(wbInValid, wbInReg.control.csrbehave, CsrBehave.no.asUInt)
  csrControl.control.csrSetmode := Mux(wbInValid, wbInReg.control.csrsetmode, CsrSetMode.origin.asUInt)
  csrControl.control.csrSource  := wbInReg.control.csrsource
  csrControl.data               := wbInReg.data

  // blackBoxHalt
  val blackBox = Module(new BlackBoxHalt);
  blackBox.io.halt     := wbInValid && wbInReg.control.goodtrap
  blackBox.io.bad_halt := wbInValid && wbInReg.control.badtrap

  wbIn.ready := true.B

  toDecode.regIndex  := Mux(wbInValid, wbInReg.data.dst, 0.U)
  toDecode.dataValid := wbInValid && wbInReg.control.regwritemux =/= RegWriteMux.no.asUInt
  toDecode.data      := regWriteIO.wdata
  toDecode.csrIndex := Mux(
    wbInValid,
    ControlRegisters.behaveDependency(wbInReg.control.csrbehave, wbInReg.control.csrsetmode, wbInReg.data.imm),
    VecInit.fill(3)(0.U(12.W))
  )
}
