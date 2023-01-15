import Chisel.{switch, Cat, Decoupled, DecoupledIO, Fill, Mux1H, MuxCase, MuxLookup}
import chisel3._
import chisel3.experimental.ChiselEnum
import chisel3.util.Enum

import scala.language.postfixOps

class Source() extends Bundle {
  val value = UInt(64.W)
  val isReg = Bool()
}

object Source {
  val default = RegInit(Source(0.U, false.B))

  def apply(value: UInt, isReg: Bool) = {
    val f = Wire(new Source())
    f.value := value
    f.isReg := isReg
    f
  }

  def apply() = new Source()
}

object OperationType extends ChiselEnum {
  val add, noMatch = Value
}

object Operation {

  val default = Operation(Source.default, Source.default, Source.default, OperationType.noMatch)

  def apply(s1: Source, s2: Source, dst: Source, opType: OperationType.Type) = {
    val op = Wire(new Operation())
    op.src1   := s1
    op.src2   := s2
    op.dst    := dst
    op.opType := opType.asUInt
    op
  }

  def apply() = new Operation()

}

class Operation() extends Bundle {
  val src1   = Source()
  val src2   = Source()
  val dst    = Source()
  val opType = UInt(2.W)
}

object InstructionType extends ChiselEnum {
  val rType, iType, sType, uType, noType = Value
}

object InstructionResType extends ChiselEnum {
  val further, noMatch, ok, other = Value
}

class Instruction() extends Bundle {
  val status          = UInt(3.W)
  val instructionType = UInt(3.W)
  val op              = Operation()
}
object Instruction {

  def further = {
    val inst = new Instruction()
    inst.status := InstructionResType.further.asUInt
    inst
  }

  val noMatch = {
    val inst = Wire(new Instruction())
    inst.status := InstructionResType.noMatch.asUInt
  }

  def apply(instType: InstructionType.Type, op: Operation) = {
    val inst = new Instruction()
    inst.status          := InstructionResType.ok
    inst.op              := op
    inst.instructionType := instType
    inst
  }

  def apply() = new Instruction

}

object Utils {
  def signalExtend(num: UInt, length: Int): UInt = {
    Cat(Fill(64 - length, num(length - 1, length - 1)), num)
  }
}

class InstructionDecodeUnit extends Module {
  val io = IO(new Bundle {
    val enable = Input(Bool())
    val inst   = Input(UInt(32.W))

  })
  val output = IO(Decoupled(Instruction()))
  val rs1    = io.inst(19, 15)
  val rs2    = io.inst(24, 20)
  val rd     = io.inst(11, 7)
  val opcode = io.inst(6, 0)
  val funct3 = io.inst(14, 12)
  val immI   = io.inst(31, 20)
  val immS   = Cat(io.inst(31, 25), io.inst(11, 7))
  val test   = Mux(opcode === 1.U, Source.default, Source.default)
  val result = MuxLookup(opcode, Instruction.further, Seq("b0010011".U -> Instruction.further))
  //    when(result.status === Instruction.further) {
  val result2 = MuxLookup(
    Cat(funct3, opcode),
    Instruction(),
    Seq(
      "b0000010011".U -> Instruction(
        InstructionType.iType,
        Operation(
          Source(rs1, true.B),
          Source(Utils.signalExtend(immI, 12), false.B),
          Source(rd, true.B),
          OperationType.add
        )
      )
    )
  )

  val finalresult = MuxLookup(
    result.status.asUInt,
    result,
    Seq(
      InstructionResType.ok.asUInt -> result,
      InstructionResType.further.asUInt -> MuxLookup(
        result2.status.asUInt,
        result2,
        Seq(
          InstructionResType.ok.asUInt -> result2
        )
      )
    )
  )

  when(io.enable) {
    output.bits  := finalresult
    output.valid := true.B
  }

}
