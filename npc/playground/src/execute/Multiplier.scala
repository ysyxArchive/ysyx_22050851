package execute

import chisel3.experimental._
import chisel3._
import chisel3.util._
import decode._
import os.read
import execute._
import utils._

class MultiplierIO extends Bundle {
  val mulValid     = Input(Bool()) //为高表示输入的数据有效，如果没有新的乘法输入，在乘法被接受的下一个周期要置低
  val flush        = Input(Bool()) //为高表示取消乘法
  val mulw         = Input(Bool()) //为高表示是 32 位乘法
  val mulSigned    = Input(UInt(2.W)) //2’b11（signed x signed）；2’b10（signed x unsigned）；2’b00（unsigned x unsigned）；
  val multiplicand = Input(UInt(64.W)) //被乘数，xlen 表示乘法器位数
  val multiplier   = Input(UInt(64.W)) //乘数
  val mulReady     = Output(Bool()) //为高表示乘法器准备好，表示可以输入数据
  val outValid     = Output(Bool()) //为高表示乘法器输出的结果有效
  val resultHigh   = Output(UInt(64.W)) //	高 xlen bits 结果
  val resultLow    = Output(UInt(64.W)) //低 xlen bits 结果
}

class SimpleMultiplier extends Module {
  val io = IO(new MultiplierIO())

  val inAReg    = Reg(UInt(64.W))
  val inBReg    = Reg(UInt(64.W))
  val outReg    = Reg(UInt(128.W))
  val isHalfMul = Reg(Bool())
  val outNeg    = Reg(Bool())

  val counter     = RegInit(0.U(6.W))
  val outValidReg = RegInit(false.B)

  val mulFire  = io.mulValid && io.mulReady
  val willDone = (!isHalfMul && counter.andR) || (isHalfMul && counter(counter.getWidth - 2, 0).andR)
  val inANeg   = io.mulSigned(1) && Mux(io.mulw, io.multiplicand(31), io.multiplicand(63))
  val inBNeg   = io.mulSigned(0) && Mux(io.mulw, io.multiplier(31), io.multiplier(63))

  val idle :: working :: others = Enum(2)
  val mulFSM = new FSM(
    idle,
    List(
      (idle, !io.flush && mulFire, working),
      (working, willDone || io.flush, idle)
    )
  )

  inAReg := Mux(
    mulFSM.is(idle),
    Mux(inANeg, Utils.signedReverse(io.multiplicand), io.multiplicand),
    inAReg
  )
  inBReg := Mux(
    mulFSM.is(idle),
    Mux(inBNeg, Utils.signedReverse(io.multiplier), io.multiplier),
    inBReg
  )
  isHalfMul := Mux(mulFSM.is(idle), io.mulw, isHalfMul)
  outNeg    := Mux(mulFSM.is(idle), inANeg ^ inBNeg, outNeg)

  counter := MuxCase(
    counter,
    Seq(
      willDone -> 0.U,
      io.flush -> 0.U,
      mulFSM.is(working) -> (counter + 1.U)
    )
  )
  outValidReg := PriorityMux(
    Seq(
      (io.flush || mulFire) -> false.B,
      willDone -> true.B,
      true.B -> outValidReg
    )
  )

  outReg := Mux(
    mulFSM.is(working),
    Mux(counter === 0.U, 0.U, outReg) + Mux(inBReg(counter), inAReg << counter, 0.U),
    outReg
  )

  io.mulReady := mulFSM.is(idle) && !io.flush
  io.outValid := outValidReg
  val out = Mux(outNeg, Utils.signedReverse(outReg), outReg)
  io.resultHigh := out >> 64
  io.resultLow  := out
}
