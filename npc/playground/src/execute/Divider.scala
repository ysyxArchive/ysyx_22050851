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

  val inAReg = Reg(UInt(128.W))
  val inBReg = Reg(UInt(64.W))
  val subReg = Reg(UInt(64.W))
  val outReg = RegInit(0.U(64.W))

  val isHalfDiv = Reg(Bool())
  val outNeg    = Reg(Bool())
  val remNeg    = Reg(Bool())

  val counter     = RegInit(0.U(6.W))
  val outValidReg = RegInit(false.B)

  val divFire  = io.divValid && io.divReady
  val willDone = (!isHalfDiv && counter.andR) || (isHalfDiv && counter(counter.getWidth - 2, 0).andR)
  val inANeg   = io.divSigned && Mux(io.divw, io.dividend(31), io.dividend(63))
  val inBNeg   = io.divSigned && Mux(io.divw, io.divisor(31), io.divisor(63))

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
  inAReg    := Mux(divFSM.is(idle), Mux(inANeg, Utils.signedReverse(inACasted), inACasted), inAReg)
  inBReg    := Mux(divFSM.is(idle), Mux(inBNeg, Utils.signedReverse(inBCasted), inBCasted), inBReg)
  isHalfDiv := Mux(divFSM.is(idle), io.divw, isHalfDiv)
  outNeg    := Mux(divFSM.is(idle), inANeg ^ inBNeg, outNeg)
  remNeg    := inANeg

  counter := MuxCase(
    counter,
    Seq(
      willDone -> 0.U,
      io.flush -> 0.U,
      divFSM.is(working) -> (counter + 1.U)
    )
  )
  outValidReg := PriorityMux(
    Seq(
      (io.flush || divFire) -> false.B,
      outValidReg -> false.B,
      willDone -> true.B,
      true.B -> outValidReg
    )
  )

  val subNext = Cat(
    subReg,
    Mux(
      isHalfDiv,
      inAReg(30.U(7.W) - counter),
      inAReg(62.U(7.W) - counter)
    )
  )
  val canSub = subNext >= inBReg
  subReg := MuxCase(
    Mux(canSub, subNext - inBReg, subNext),
    Seq(
      (divFSM.is(idle) && io.divw) -> Mux(inANeg, Utils.signedReverse(inACasted), inACasted)(125, 31),
      (divFSM.is(idle) && !io.divw) -> Mux(inANeg, Utils.signedReverse(inACasted), inACasted)(125, 63),
      divFSM.willChangeTo(output) -> subReg,
    )
  )
  outReg      := Mux(divFSM.is(working) && !willDone, Cat(outReg, canSub), Mux(divFSM.is(idle), 0.U, outReg))
  io.divReady := divFSM.is(idle) && !io.flush
  io.outValid := divFSM.is(output)
  val out = Mux(outNeg, Utils.signedReverse(outReg.asUInt), outReg.asUInt)
  val sub = Mux(remNeg, Utils.signedReverse(subReg), subReg)
  io.quotient  := out
  io.remainder := sub
}
