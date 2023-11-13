package execute

import chisel3.experimental._
import chisel3._
import chisel3.util._
import decode._
import os.read
import execute._
import utils._
import upickle.core.Util

class DividerIO extends Bundle {
  val dividend  = Input(UInt(64.W)) // 被除数（ xlen 表示要实现的位数，ysyx 中是 64）
  val divisor   = Input(UInt(64.W)) // 除数
  val divValid  = Input(Bool()) // 为高表示输入的数据有效，如果没有新的除法输入，在除法被接受的下一个周期要置低
  val divw      = Input(Bool()) // 为高表示输入的是 32 位除法
  val divSigned = Input(Bool()) // 表示是不是有符号除法，为高表示是有符号除法
  val flush     = Input(Bool()) // 为高表示要取消除法（修改一下除法器状态就行）
  val divReady  = Output(Bool()) // 为高表示除法器空闲，可以输入数据
  val outValid  = Output(Bool()) // 为高表示除法器输出了有效结果
  val quotient  = Output(UInt(64.W)) // 商
  val remainder = Output(UInt(64.W)) // 余数
}

class SimpleDivider extends Module {
  val io = IO(new DividerIO())

  val subReg = Reg(UInt(64.W))
  val outReg = RegInit(0.U(64.W))

  val counter = RegInit(0.U(6.W))

  val divFire  = io.divValid && io.divReady
  val willDone = (!io.divw && !counter.orR) || (io.divw && !counter(counter.getWidth - 2, 0).orR)
  val inANeg   = io.divSigned && Mux(io.divw, io.dividend(31), io.dividend(63))
  val inBNeg   = io.divSigned && Mux(io.divw, io.divisor(31), io.divisor(63))
  val outNeg   = inANeg ^ inBNeg
  val remNeg   = inANeg

  val idle :: working :: output :: others = Enum(10)
  val divFSM = new FSM(
    idle,
    List(
      (idle, !io.flush && divFire, working),
      (working, willDone, output),
      (working, io.flush, idle),
      (output, true.B, idle)
    )
  )

  val inACasted = Mux(
    io.divw,
    Mux(io.divSigned, Utils.signExtend(io.dividend, 32, 128), Utils.zeroExtend(io.dividend, 32, 128)),
    Mux(io.divSigned, Utils.signExtend(io.dividend, 64, 128), Utils.zeroExtend(io.dividend, 64, 128))
  )
  val inBCasted = Mux(
    io.divw,
    Mux(io.divSigned, Utils.signExtend(io.divisor, 32, 64), Utils.zeroExtend(io.divisor, 32, 64)),
    io.divisor
  )
  val inA = Mux(inANeg, Utils.signedReverse(inACasted), inACasted)
  val inB = Mux(inBNeg, Utils.signedReverse(inBCasted), inBCasted)

  counter := MuxCase(
    counter,
    Seq(
      io.flush -> 0.U,
      (divFSM.is(working) || divFSM.willChangeTo(working)) -> (counter + 1.U)
    )
  )

  val subNext = Mux(
    divFSM.is(working),
    Cat(subReg, Mux(io.divw, inA(30.U(7.W) - counter), inA(62.U(7.W) - counter))),
    Mux(io.divw, inA(63, 32), inA(127, 64))
  )
  val canSub = subNext >= inB
  subReg := Mux(canSub, subNext - inB, subNext)

  outReg      := Mux(divFSM.is(working) && !willDone, Cat(outReg, canSub), Mux(divFSM.is(idle), 0.U, outReg))
  
  io.divReady := divFSM.is(idle) && !io.flush
  io.outValid := divFSM.is(output)
  
  val out = Mux(outNeg, Utils.signedReverse(outReg.asUInt), outReg.asUInt)
  val sub = Mux(remNeg, Utils.signedReverse(subReg), subReg)
  io.quotient  := out
  io.remainder := sub
}
