import chisel3._
import chisel3.experimental.BundleLiterals._
import chisel3.util._
import utils._
import decode._
import execute._

class DecodeIn extends Bundle {
  val debug = Output(new DebugInfo)
  val pc    = Output(UInt(64.W))
  val inst  = Output(UInt(32.W))
}

class DecodeBack extends Bundle {
  val willTakeBranch = Output(Bool())
  val branchPc       = Output(UInt(64.W))
}

class InstructionDecodeUnit extends Module {
  val regIO      = IO(Flipped(new RegReadIO()))
  val decodeIn   = IO(Flipped(Decoupled(new DecodeIn())))
  val decodeOut  = IO(Decoupled(new ExeIn()))
  val decodeBack = IO(new DecodeBack())
  val fromExe    = IO(Input(UInt(5.W)))
  val fromMemu   = IO(Input(UInt(5.W)))
  val fromWbu    = IO(Input(UInt(5.W)))

  val controlDecoder = Module(new InstContorlDecoder)

  val decodeInReg = Reg(new DecodeIn())

  val willTakeBranch = Wire(Bool())
  val shouldWait     = Wire(Bool())

  val waitFetch :: waitSend :: bubble :: others = Enum(4)
  val decodeFSM = new FSM(
    waitFetch,
    List(
      (waitSend, decodeOut.fire && shouldWait, waitSend),
      (waitSend, decodeOut.fire && !willTakeBranch, waitFetch),
      (waitSend, decodeOut.fire && willTakeBranch, bubble),
      (bubble, true.B, waitFetch),
      (waitFetch, decodeIn.fire, waitSend)
    )
  )

  decodeInReg := Mux(decodeIn.fire, decodeIn.bits, decodeInReg)

  // decodeout.control
  controlDecoder.input := decodeInReg.inst

  // decodeout.data
  val inst = decodeInReg.inst
  val rs1  = inst(19, 15)
  val rs2  = inst(24, 20)
  val rd   = inst(11, 7)
  val immI = Utils.signExtend(inst(31, 20), 12)
  val immS = Utils.signExtend(Cat(inst(31, 25), inst(11, 7)), 12)
  val immU = Utils.signExtend(inst(31, 12), 20) << 12
  val immB = Cat(Utils.signExtend(inst(31), 1), inst(7), inst(30, 25), inst(11, 8), 0.U(1.W))
  val immJ = Utils.signExtend(
    Cat(inst(31), inst(19, 12), inst(20), inst(30, 21), 0.U(1.W)),
    20
  );
  val imm = MuxLookup(controlDecoder.output.insttype, immI)(
    EnumSeq(
      InstType.I -> immI,
      InstType.S -> immS,
      InstType.U -> immU,
      InstType.B -> immB,
      InstType.J -> immJ
    )
  )
  decodeOut.bits.data.imm  := imm
  decodeOut.bits.data.src1 := rs1
  decodeOut.bits.data.src2 := rs2
  decodeOut.bits.data.dst  := rd

  decodeOut.valid        := decodeFSM.is(waitSend)
  decodeOut.bits.data.pc := decodeInReg.pc
  decodeOut.bits.control := controlDecoder.output

  decodeIn.ready := decodeFSM.is(waitFetch)

  // regIO
  regIO.raddr0 := rs1
  regIO.raddr1 := rs2
  val src1Data = Mux(
    controlDecoder.output.srccast1,
    Utils.cast(regIO.out0, 32, 64),
    regIO.out0
  )
  val src2Data = Mux(
    controlDecoder.output.srccast2,
    Utils.cast(regIO.out1, 32, 64),
    regIO.out1
  )
  decodeOut.bits.data.src1Data := src1Data
  decodeOut.bits.data.src2Data := src2Data
  Mux(
    controlDecoder.output.srccast2,
    Utils.cast(regIO.out1, 32, 64),
    regIO.out1
  )

  // RAW check
  val dstVec = VecInit(fromExe, fromMemu, fromWbu)
  shouldWait            := (rs1 =/= 0.U && dstVec.contains(rs1)) || (rs2 =/= 0.U && dstVec.contains(rs2))
  decodeOut.bits.enable := !shouldWait

  // branch check
  willTakeBranch := MuxLookup(controlDecoder.output.pcaddrsrc, false.B)(
    EnumSeq(
      PCAddrSrc.aluzero -> (src1Data === src2Data),
      PCAddrSrc.alunotneg -> (src1Data.asSInt >= src2Data.asSInt),
      PCAddrSrc.alucarryorzero -> (src1Data >= src2Data),
      PCAddrSrc.aluneg -> (src1Data.asSInt < src2Data.asSInt),
      PCAddrSrc.alunotcarryandnotzero -> (src1Data < src2Data),
      PCAddrSrc.alunotzero -> (src1Data =/= src2Data),
      PCAddrSrc.one -> true.B
    )
  )
  val branchPc = MuxLookup(controlDecoder.output.pcsrc, 0.U)(
    EnumSeq(
      PcSrc.pc -> decodeInReg.pc,
      PcSrc.src1 -> src1Data
    )
  ) + imm

  decodeBack.willTakeBranch := willTakeBranch
  decodeBack.branchPc       := branchPc
  
  decodeOut.bits.data.dnpc  := Mux(shouldWait, decodeInReg.pc, Mux(willTakeBranch, branchPc, decodeInReg.pc + 4.U))
  // debug
  decodeOut.bits.debug.pc   := decodeInReg.debug.pc
  decodeOut.bits.debug.inst := inst

}
