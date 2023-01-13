import Chisel.{Cat, Decoupled, DecoupledIO, Fill, Mux1H, MuxCase, MuxLookup, switch}
import chisel3.DontCare.:=
import chisel3._
import chisel3.experimental.ChiselEnum
import chisel3.util.Enum

import scala.language.postfixOps

object Source {
  def apply() = new Source()
}

class Source(val value: UInt = UInt(64.W), val isReg: Bool = Bool()) {}

object OperationType extends ChiselEnum {
  val add, noMatch = Value
}

object Operation {
  def apply() = new Operation(Source(), Source(), Source(), OperationType.noMatch)
}

class Operation(val src1: Source, val src2: Source, val dst: Source, val opType: OperationType.Type) extends Bundle {}

object InstructionType extends ChiselEnum {
  val rType, iType, sType, uType, noType = Value
}

object InstructionResType extends ChiselEnum {
  val further, noMatch, ok, other = Value
}

object Instruction {

  def apply() = new Instruction()

  def apply(isFurther: Bool) = new Instruction(Mux(isFurther, InstructionResType.further.asUInt, InstructionResType.noMatch.asUInt), InstructionType.noType.asUInt, Operation())

  def apply(instType: InstructionType.Type, ops: Seq[Operation]) = new Instruction(InstructionResType.ok.asUInt, instType.asUInt, Operation())
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

  val result: Instruction = MuxLookup(opcode, Instruction(), Seq("b0010011".U -> Instruction(true.B)))
  //    when(result.status === Instruction.further) {
  val result2 = MuxLookup(Cat(funct3, opcode), Instruction(), Seq(
    "b0000010011".U -> Instruction(
      InstructionType.iType, Seq(
        new Operation(
          new Source(rs1, true.B),
          new Source(Utils.signalExtend(immI, 12), false.B),
          new Source(rd, true.B),
          OperationType.add
        )
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
