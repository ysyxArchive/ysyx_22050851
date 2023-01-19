import chisel3._
import chisel3.experimental.ChiselEnum
import chisel3.util._

import scala.language.postfixOps
import org.apache.commons.lang3.ObjectUtils

object SourceType extends ChiselEnum {
  val reg, imm, pc = Value
}

class Source() extends Bundle {
  val value = UInt(64.W)
  val stype = UInt(SourceType.getWidth.W)
}

object Source {
  val default = RegInit(Source(0.U, SourceType.imm))

  def apply(value: UInt, isReg: SourceType.Type) = {
    val f = Wire(new Source())
    f.value := value
    f.stype := isReg.asUInt
    f
  }

  def apply() = new Source()
}

object OperationType extends ChiselEnum {
  val add, move, halt, noMatch = Value
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
  val opType = UInt(OperationType.getWidth.W)
}

object InstructionType extends ChiselEnum {
  val rType, iType, sType, uType, noType = Value
}

object InstructionResType extends ChiselEnum {
  val further, noMatch, ok, other = Value
}

class Instruction() extends Bundle {
  val status          = UInt(InstructionResType.getWidth.W)
  val instructionType = UInt(InstructionType.getWidth.W)
  val op              = Operation()
}
object Instruction {

  def further = {
    val inst = Wire(new Instruction())
    inst.status          := InstructionResType.further.asUInt
    inst.instructionType := DontCare
    inst.op              := DontCare
    inst
  }

  val noMatch = {
    val inst = Wire(new Instruction())
    inst.status          := InstructionResType.noMatch.asUInt
    inst.instructionType := DontCare
    inst.op              := DontCare
    inst
  }

  def apply(instType: InstructionType.Type, op: Operation) = {
    val inst = Wire(new Instruction())
    inst.status          := InstructionResType.ok.asUInt
    inst.op              := op
    inst.instructionType := instType.asUInt
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
  val output = IO(Decoupled(Operation()))

  val debugp = IO(Output(UInt(32.W)))

  val resultValid = RegInit(false.B)
  output.valid := !output.ready && resultValid
  output.bits  := DontCare

  val rs1    = io.inst(19, 15)
  val rs2    = io.inst(24, 20)
  val rd     = io.inst(11, 7)
  val opcode = io.inst(6, 0)
  val funct3 = io.inst(14, 12)
  val immI   = io.inst(31, 20)
  val immS   = Cat(io.inst(31, 25), io.inst(11, 7))

  val result = MuxLookup(
    opcode,
    Instruction.further,
    Seq("b0010011".U -> Instruction.further, "b1110011".U -> Instruction.further)
  )
  val result2 = MuxLookup(
    Cat(funct3, opcode),
    Instruction.noMatch,
    Seq(
      "b0001110011".U -> Instruction.further,
      "b0000010011".U -> Instruction(
        InstructionType.iType,
        Operation(
          Source(rs1, SourceType.reg),
          Source(Utils.signalExtend(immI, 12), SourceType.imm),
          Source(rd, SourceType.reg),
          OperationType.add
        )
      )
    )
  )
  val result3 = MuxLookup(
    io.inst,
    Instruction.noMatch,
    Seq(
      "b00000000000100000000000001110011".U -> Instruction(
        InstructionType.iType,
        Operation(
          Source(0.U, SourceType.imm),
          Source(3.U, SourceType.imm),
          Source(3.U, SourceType.pc),
          OperationType.halt
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
          InstructionResType.ok.asUInt -> result2,
          InstructionResType.further.asUInt -> MuxLookup(
            result3.status.asUInt,
            result3,
            Seq(
              InstructionResType.ok.asUInt -> result3
            )
          )
        )
      )
    )
  )
  debugp := Cat(result.status, result2.status, result3.status)
  when(io.enable) {
    output.enq(finalresult.op)
  }

}
