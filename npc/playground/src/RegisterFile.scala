import chisel3._
import chisel3.util.{is, switch, MuxLookup}

import scala.language.postfixOps

/**
  * Compute GCD using subtraction method.
  * Subtracts the smaller from the larger until register y is zero.
  * value in register x is then the GCD
  */

class RegisterFileIO extends Bundle {
  val wdata  = Input(UInt(64.W))
  val waddr  = Input(UInt(5.W))
  val wen    = Input(Bool())
  val out1   = Output(UInt(64.W))
  val raddr1 = Input(UInt(5.W))
  val out2   = Output(UInt(64.W))
  val raddr2 = Input(UInt(5.W))
  val pc     = Output(UInt(64.W))
}

class RegisterFile extends Module {
  val io   = IO(new RegisterFileIO())
  val regs = RegInit(VecInit(Seq.fill(32)(0.U(64.W))))
  val pc: UInt = RegNext(pc + 4.U, "h80000000".asUInt(64.W))

  io.pc := pc

  when(io.wen && io.waddr =/= 0.U) {
    regs(io.waddr) := io.wdata
  }

  io.pc   := pc
  io.out1 := Mux(io.raddr1 === 0.U, 0.U, Mux(io.raddr1 === io.waddr && io.wen, io.wdata, regs(io.raddr1)))
  io.out2 := Mux(io.raddr2 === 0.U, 0.U, Mux(io.raddr2 === io.waddr && io.wen, io.wdata, regs(io.raddr2)))

}
