import Chisel.{Cat, Decoupled, DecoupledIO, Fill, Mux1H, MuxCase, MuxLookup, switch}
import chisel3.DontCare.:=
import chisel3._
import chisel3.experimental.ChiselEnum
import chisel3.util.Enum

import scala.language.postfixOps

object Source {
  val default = apply()


  def apply(value: UInt = 0.U, isReg: Bool = false.B) = {
    val f = new Source()
    f.value := value
    f.isReg := isReg
    f
  }
}

class Source() extends Bundle {
  val value = UInt(64.W)
  val isReg = Bool()
}

object OperationType extends ChiselEnum {
  val add, noMatch = Value
}

object Operation {

  val default = new Operation(Source.default, Source.default, Source.default, OperationType.noMatch.asUInt)

  def apply(s1: Source, s2: Source, dst: Source, opType: OperationType.Type) = new Operation(s1, s2, dst, opType.asUInt)

  def apply() = new Operation(Source(), Source(), Source())

}

class Operation(val src1: Source, val src2: Source, val dst: Source, val opType: UInt = UInt(2.W)) extends Bundle {}

object InstructionType extends ChiselEnum {
  val rType, iType, sType, uType, noType = Value
}

object InstructionResType extends ChiselEnum {
  val further, noMatch, ok, other = Value
}

object Instruction {


  val further = new Instruction(InstructionResType.further.asUInt, InstructionType.noType.asUInt, Operation.default)
  val noMatch = new Instruction(InstructionResType.noMatch.asUInt, InstructionType.noType.asUInt, Operation.default)

  def apply() = new Instruction()


  def apply(instType: InstructionType.Type, op: Operation) = new Instruction(InstructionResType.ok.asUInt, instType.asUInt, op)
}

class Instruction(val status: UInt = UInt(3.W), val instructionType: UInt = UInt(3.W), val op: Operation = Operation()) extends Bundle {
  /** no match Instruction */

}

object Utils {
  def signalExtend(num: UInt, length: Int): UInt = {
    Cat(Fill(64 - length, num(length - 1, length - 1)), num)
  }
}


class InstructionDecodeUnit extends Module {
  val io = IO(new Bundle {
    val enable = Input(Bool())
    val inst = Input(UInt(32.W))

  })
  val output = IO(Decoupled(Instruction()))
  val rs1 = io.inst(19, 15)
  val rs2 = io.inst(24, 20)
  val rd = io.inst(11, 7)
  val opcode = io.inst(6, 0)
  val funct3 = io.inst(14, 12)
  val immI = io.inst(31, 20)
  val immS = Cat(io.inst(31, 25), io.inst(11, 7))
  val test = Mux(opcode === 1.U, Source.default, Source.default)
  val result = MuxLookup(opcode, Instruction.further, Seq("b0010011".U -> Instruction.further))
  //    when(result.status === Instruction.further) {
  val result2 = MuxLookup(Cat(funct3, opcode), Instruction(), Seq(
    "b0000010011".U -> Instruction(
      InstructionType.iType, Operation(
        Source(rs1, true.B),
        Source(Utils.signalExtend(immI, 12), false.B),
        Source(rd, true.B),
        OperationType.add
      )

    )
  ))

  val finalresult = MuxLookup(result.status.asUInt, result, Seq(
    InstructionResType.ok.asUInt -> result,
    InstructionResType.further.asUInt -> MuxLookup(result2.status.asUInt, result2, Seq(
      InstructionResType.ok.asUInt -> result2,
    ))
  ))

  when(io.enable) {
    output.bits := finalresult
    output.valid := true.B
  }

}
