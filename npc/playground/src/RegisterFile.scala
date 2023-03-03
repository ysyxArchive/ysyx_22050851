import chisel3._
import chisel3.util.{is, switch, MuxLookup}

import scala.language.postfixOps

/**
  * Compute GCD using subtraction method.
  * Subtracts the smaller from the larger until register y is zero.
  * value in register x is then the GCD
  */

class RegisterFileIO extends Bundle {
  val wdata = Input(UInt(64.W))
  val waddr = Input(UInt(5.W))

  val dnpc    = Input(UInt(64.W))
  val pcWrite = Input(Bool())

  val out0   = Output(UInt(64.W))
  val raddr0 = Input(UInt(5.W))

  val out1   = Output(UInt(64.W))
  val raddr1 = Input(UInt(5.W))

  val pc  = Output(UInt(64.W))
  val npc = Output(UInt(64.W))
}

class RegisterFile extends Module {
  val io          = IO(new RegisterFileIO())
  val next        = IO(new Bundle { val next = Input(Bool()) })
  val blackBoxOut = Module(new BlackBoxRegs);

  val pc = Wire(UInt(64.W))

  val regs   = RegInit(VecInit(Seq.fill(32)(0.U(64.W))))
  val pcLast = Reg(UInt(64.W))

  blackBoxOut.io.pc   := pcLast;
  blackBoxOut.io.regs := regs;

  pc := RegNext(Mux(io.pcWrite, io.dnpc, pc + 4.U), "h80000000".asUInt(64.W))

  pcLast := Mux(next.next, pc, pcLast);

  for (i <- 0 to 31) {
    regs(i) := Mux(io.waddr === i.U, io.wdata, regs(i))
  }

  io.out0 := Mux(io.raddr0 === 0.U, 0.U, regs(io.raddr0))
  io.out1 := Mux(io.raddr1 === 0.U, 0.U, regs(io.raddr1))
  io.pc   := pcLast
  io.npc  := pc

}
