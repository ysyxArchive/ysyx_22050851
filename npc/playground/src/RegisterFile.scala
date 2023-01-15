import chisel3._
import chisel3.util.{MuxLookup, is, switch}

import scala.language.postfixOps

/**
 * Compute GCD using subtraction method.
 * Subtracts the smaller from the larger until register y is zero.
 * value in register x is then the GCD
 */

class RegisterFileIO extends Bundle {
  val wdata = Input(UInt(64.W))
  val waddr = Input(UInt(5.W))
  val wen = Input(Bool())
  val rdata1 = Output(UInt(64.W))
  val raddr1 = Input(UInt(5.W))
  val rdata2 = Output(UInt(64.W))
  val raddr2 = Input(UInt(5.W))
}

class RegisterFile extends Module {
  val io = IO(new RegisterFileIO())

  val regs: Vec[UInt] = RegInit(VecInit(Seq.fill(32)(0.U(64.W))))

  when(io.wen && io.waddr =/= 0.U) {
    regs(io.waddr) := io.wdata
  }

  io.rdata1 := Mux(io.raddr1 === 0.U, 0.U, Mux(io.raddr1 === io.waddr && io.wen, io.wdata, regs(io.raddr1)))
  io.rdata2 := Mux(io.raddr2 === 0.U, 0.U, Mux(io.raddr2 === io.waddr && io.wen, io.wdata, regs(io.raddr2)))

}
