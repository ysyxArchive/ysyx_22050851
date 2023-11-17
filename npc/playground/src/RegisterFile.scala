import chisel3._
import chisel3.util.{is, switch, MuxLookup}

import scala.language.postfixOps

class RegWriteIO extends Bundle {
  val wdata = Input(UInt(64.W))
  val waddr = Input(UInt(5.W))

  val dnpc = Input(UInt(64.W))
}
class RegReadIO extends Bundle {
  val out0   = Output(UInt(64.W))
  val raddr0 = Input(UInt(5.W))

  val out1   = Output(UInt(64.W))
  val raddr1 = Input(UInt(5.W))

  val pc = Output(UInt(64.W))
}

class RegisterFile extends Module {
  val readIO     = IO(new RegReadIO())
  val writeIO    = IO(new RegWriteIO())
  val debugOut   = IO(Output(Vec(32, UInt(64.W))))
  val debugPCOut = IO(Output(UInt(64.W)))

  val pc = RegNext(writeIO.dnpc, "h80000000".asUInt(64.W))

  val regs = RegInit(VecInit(Seq.fill(32)(0.U(64.W))))

  for (i <- 0 to 31) {
    regs(i) := Mux(writeIO.waddr === i.U, writeIO.wdata, regs(i))
  }

  debugOut   := regs
  debugPCOut := pc

  readIO.out0 := Mux(readIO.raddr0 === 0.U, 0.U, regs(readIO.raddr0))
  readIO.out1 := Mux(readIO.raddr1 === 0.U, 0.U, regs(readIO.raddr1))
  readIO.pc   := pc

}
