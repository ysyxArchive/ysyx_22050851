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

class BoothModule extends Module {
  val io = IO(new Bundle {
    val in          = Input(UInt(3.W))
    val isNeg       = Output(Bool())
    val isWork      = Output(Bool())
    val shouldShift = Output(Bool())
  })
  io.isNeg       := io.in(2)
  io.isWork      := !(io.in.andR || (~io.in).andR)
  io.shouldShift := (io.in(0) ^ io.in(2)) && (io.in(1) ^ io.in(2))
}

class BoothMultiplier extends Module {
  val io = IO(new MultiplierIO())

  val booth = Module(new BoothModule())

  val outReg = Reg(UInt(128.W))

  val counter = RegInit(0.U(5.W))

  val mulFire  = io.mulValid && io.mulReady
  val willDone = (!io.mulw && counter.andR) || (io.mulw && counter(counter.getWidth - 2, 0).andR)

  val idle :: working :: output :: others = Enum(3)
  val mulFSM = new FSM(
    idle,
    List(
      (idle, !io.flush && mulFire, working),
      (working, willDone, output),
      (working, io.flush, idle),
      (output, true.B, idle)
    )
  )

  val inA    = Mux(io.mulSigned(1), Utils.signExtend(io.multiplicand, 64, 128), Utils.zeroExtend(io.multiplicand, 64, 128))
  val inB    = Cat(io.multiplier, 0.U(1.W))
  val inANeg = Utils.signedReverse(inA)

  counter := MuxCase(
    counter,
    Seq(
      willDone -> 0.U,
      io.flush -> 0.U,
      (mulFSM.is(working) || mulFSM.willChangeTo(working)) -> (counter + 1.U)
    )
  )

  booth.io.in := inB >> ((Mux(io.mulw, 15.U, 31.U) - counter) * 2.U)
  val valToAdd = MuxCase(
    0.U,
    Seq(
      !booth.io.isWork -> 0.U,
      (booth.io.isWork && booth.io.isNeg) -> inANeg,
      (booth.io.isWork && !booth.io.isNeg) -> inA
    )
  ) << booth.io.shouldShift
  outReg := Mux(mulFSM.is(working), (outReg << 2.U) + valToAdd, valToAdd)

  io.mulReady   := mulFSM.is(idle) && !io.flush
  io.outValid   := mulFSM.is(output)
  io.resultHigh := (outReg >> 64) + Mux(!io.mulSigned(0) && inB(64), inA, 0.U)
  io.resultLow  := outReg
}

class WallaceLayer(inputCnt: Int) extends Module {
  val fullAdderCnt = inputCnt / 3
  val halfAdderCnt = inputCnt % 3 / 2
  val outSWidth    = fullAdderCnt + halfAdderCnt + inputCnt % 3 % 2
  val outCWidth    = fullAdderCnt + halfAdderCnt
  val io = IO(new Bundle {
    val bits = Input(UInt(inputCnt.W))
    val outC = Output(UInt(outCWidth.W))
    val outS = Output(UInt(outSWidth.W))
  })

  val outS = Wire(Vec(outSWidth, Bool()))
  val outC = Wire(Vec(outCWidth, Bool()))

  val fullAdders = Seq.fill(fullAdderCnt)(Module(new FullAdder()))
  for (i <- 0 until fullAdderCnt) {
    val adder = fullAdders(i)
    adder.io.inA := io.bits(i * 3)
    adder.io.inB := io.bits(i * 3 + 1)
    adder.io.inC := io.bits(i * 3 + 2)
    outS(i)      := adder.io.out
    outC(i)      := adder.io.outC
  }
  if (halfAdderCnt > 0) {
    outS(fullAdderCnt) := io.bits(fullAdderCnt * 3) ^ io.bits(fullAdderCnt * 3 + 1)
    outC(fullAdderCnt) := io.bits(fullAdderCnt * 3) && io.bits(fullAdderCnt * 3 + 1)
  }
  if (inputCnt % 3 % 2 > 0) {
    outS(fullAdderCnt + halfAdderCnt) := io.bits(fullAdderCnt * 3 + halfAdderCnt * 2)
  }
  io.outC := outC.asUInt
  io.outS := outS.asUInt
}
class BHMultiplier extends Module {
  val io = IO(new MultiplierIO())

  val mulFire  = io.mulValid && io.mulReady
  val willDone = (!io.mulw && true.B) || (io.mulw && true.B)

  val idle :: step1 :: step2 :: step3 :: step4 :: step5 :: step6 :: step7 :: step8 :: output :: others = Enum(10)
  val mulFSM = new FSM(
    idle,
    List(
      (idle, mulFire && !io.mulw, step1),
      (idle, mulFire && io.mulw, step3),
      (step1, true.B, step2),
      (step2, true.B, step3),
      (step3, true.B, step4),
      (step4, true.B, step5),
      (step5, true.B, step6),
      (step6, true.B, step7),
      (step7, true.B, step8),
      (step8, true.B, output),
      (output, true.B, idle)
    )
  )

  val inA    = Mux(io.mulSigned(1), Utils.signExtend(io.multiplicand, 64, 128), Utils.zeroExtend(io.multiplicand, 64, 128))
  val inB    = Cat(io.multiplier, 0.U(1.W))
  val inANeg = Utils.signedReverse(inA)

  val booths = Seq.fill(32)(Module(new BoothModule()))

  val boothWork  = Reg(Vec(32, Bool()))
  val boothNeg   = Reg(Vec(32, Bool()))
  val boothShift = Reg(Vec(32, Bool()))
  val addBuffer  = Reg(Vec(32, UInt(128.W)))

  for (i <- 0 until 32) {
    booths(i).io.in := inB(i * 2 + 2, i * 2)
    val out = booths(i).io
    boothWork(i)  := Mux(mulFSM.is(idle), out.isWork, boothWork(i))
    boothNeg(i)   := Mux(mulFSM.is(idle), out.isNeg, boothNeg(i))
    boothShift(i) := Mux(mulFSM.is(idle), out.shouldShift, boothShift(i))
    addBuffer(i) := Mux(
      mulFSM.is(idle),
      Mux(out.isWork, Mux(out.isNeg, inANeg, inA), 0.U) << (out.shouldShift + (i * 2).U),
      addBuffer(i)
    )
  }
  val wallaceLayer1 = Seq.fill(128)(Module(new WallaceLayer(32)))
  val sBuffer1      = Reg(Vec(128, UInt(11.W)))
  val cBuffer1      = Reg(Vec(128, UInt(11.W)))
  for (i <- 0 until 128) {
    wallaceLayer1(i).io.bits := VecInit(addBuffer.map(v => v(i))).asUInt
    sBuffer1(i)              := wallaceLayer1(i).io.outS
    cBuffer1(i)              := wallaceLayer1(i).io.outC
  }
  val wallaceLayer2 = Seq.fill(128)(Module(new WallaceLayer(22)))
  val sBuffer2      = Reg(Vec(128, UInt(8.W)))
  val cBuffer2      = Reg(Vec(128, UInt(7.W)))
  for (i <- 0 until 128) {
    if (i == 0) {
      wallaceLayer2(i).io.bits := sBuffer1(i)
    } else {
      wallaceLayer2(i).io.bits := Cat(cBuffer1(i - 1), sBuffer1(i))
    }
    sBuffer2(i) := wallaceLayer2(i).io.outS
    cBuffer2(i) := wallaceLayer2(i).io.outC
  }

  val wallaceLayer3 = Seq.fill(128)(Module(new WallaceLayer(16)))
  val sBuffer3      = Reg(Vec(128, UInt(6.W)))
  val cBuffer3      = Reg(Vec(128, UInt(5.W)))
  for (i <- 0 until 128) {
    if (i == 0) {
      wallaceLayer3(i).io.bits := Mux(io.mulw, VecInit(addBuffer.map(v => v(i))).asUInt, sBuffer2(i))
    } else {
      wallaceLayer3(i).io.bits := Mux(
        io.mulw,
        VecInit(addBuffer.map(v => v(i))).asUInt,
        Cat(cBuffer2(i - 1), sBuffer2(i))
      )
    }
    sBuffer3(i) := wallaceLayer3(i).io.outS
    cBuffer3(i) := wallaceLayer3(i).io.outC
  }

  val wallaceLayer4 = Seq.fill(128)(Module(new WallaceLayer(11)))
  val sBuffer4      = Reg(Vec(128, UInt(4.W)))
  val cBuffer4      = Reg(Vec(128, UInt(4.W)))
  for (i <- 0 until 128) {
    if (i == 0) {
      wallaceLayer4(i).io.bits := sBuffer3(i)
    } else {
      wallaceLayer4(i).io.bits := Cat(cBuffer3(i - 1), sBuffer3(i))
    }
    sBuffer4(i) := wallaceLayer4(i).io.outS
    cBuffer4(i) := wallaceLayer4(i).io.outC
  }

  val wallaceLayer5 = Seq.fill(128)(Module(new WallaceLayer(8)))
  val sBuffer5      = Reg(Vec(128, UInt(3.W)))
  val cBuffer5      = Reg(Vec(128, UInt(3.W)))
  for (i <- 0 until 128) {
    if (i == 0) {
      wallaceLayer5(i).io.bits := sBuffer4(i)
    } else {
      wallaceLayer5(i).io.bits := Cat(cBuffer4(i - 1), sBuffer4(i))
    }
    sBuffer5(i) := wallaceLayer5(i).io.outS
    cBuffer5(i) := wallaceLayer5(i).io.outC
  }

  val wallaceLayer6 = Seq.fill(128)(Module(new WallaceLayer(6)))
  val sBuffer6      = Reg(Vec(128, UInt(2.W)))
  val cBuffer6      = Reg(Vec(128, UInt(2.W)))
  for (i <- 0 until 128) {
    if (i == 0) {
      wallaceLayer6(i).io.bits := sBuffer5(i)
    } else {
      wallaceLayer6(i).io.bits := Cat(cBuffer5(i - 1), sBuffer5(i))
    }
    sBuffer6(i) := wallaceLayer6(i).io.outS
    cBuffer6(i) := wallaceLayer6(i).io.outC
  }

  val wallaceLayer7 = Seq.fill(128)(Module(new WallaceLayer(4)))
  val sBuffer7      = Reg(Vec(128, UInt(2.W)))
  val cBuffer7      = Reg(Vec(128, UInt(1.W)))
  for (i <- 0 until 128) {
    if (i == 0) {
      wallaceLayer7(i).io.bits := sBuffer6(i)
    } else {
      wallaceLayer7(i).io.bits := Cat(cBuffer6(i - 1), sBuffer6(i))
    }
    sBuffer7(i) := wallaceLayer7(i).io.outS
    cBuffer7(i) := wallaceLayer7(i).io.outC
  }

  val wallaceLayer8 = Seq.fill(128)(Module(new WallaceLayer(3)))
  val sBuffer8      = Reg(Vec(128, UInt(1.W)))
  val cBuffer8      = Reg(Vec(128, UInt(1.W)))
  for (i <- 0 until 128) {
    if (i == 0) {
      wallaceLayer8(i).io.bits := sBuffer6(i)
    } else {
      wallaceLayer8(i).io.bits := Cat(cBuffer7(i - 1), sBuffer7(i))
    }
    sBuffer8(i) := wallaceLayer8(i).io.outS
    cBuffer8(i) := wallaceLayer8(i).io.outC
  }

  val out = sBuffer8.asUInt + (cBuffer8.asUInt << 1)

  io.mulReady   := mulFSM.is(idle) && !io.flush
  io.outValid   := mulFSM.is(output)
  io.resultHigh := (out >> 64) + Mux(!io.mulSigned(0) && inB(64), inA, 0.U)
  io.resultLow  := out
}
