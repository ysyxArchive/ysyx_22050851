package decode

import chisel3._
import chisel3.experimental.ChiselEnum
import chisel3.util._
import scala.language.postfixOps
import org.apache.commons.lang3.ObjectUtils
import execute._

object SourceType extends ChiselEnum {
  val reg, regLow, imm, pc, npc, mem, alu, aluSign, temp = Value
}

class Source() extends Bundle {
  val value = UInt(64.W)
  val stype = UInt(SourceType.getWidth.W)
}

object Source {
  val default = Source(0.U, SourceType.imm)
  val npc     = Source(0.U, SourceType.npc)
  val pc      = Source(ALUUtils.none, SourceType.pc)
  val alu     = Source(0.U, SourceType.alu)
  val mem     = Source(0.U, SourceType.mem)
  val temp    = Source(0.U, SourceType.temp)

  def aluSign(aluSignalType: ALUSignalType.Type) = Source(aluSignalType.asUInt, SourceType.aluSign)
  def alu(aluType:           ALUType.Type)       = Source(aluType.asUInt, SourceType.alu)
  def pc(check:              UInt)               = Source(check, SourceType.pc)
  def reg(index:             UInt)               = Source(index, SourceType.reg)
  def regLow(index:          UInt)               = Source(index, SourceType.regLow)
  def imm(num:               UInt)               = Source(num, SourceType.imm)
  def mem(num:               UInt)               = Source(num, SourceType.imm)

  def apply(value: UInt, stype: SourceType.Type) = {
    val f = Wire(new Source())
    f.value := value
    f.stype := stype.asUInt
    f
  }

  def apply() = new Source()
}

object OperationType extends ChiselEnum {
  val mul, divS, div, remS, rem, alu, move, savemem, loadmemU, loadmemS, halt, noMatch, updatePC, nothing = Value
}

object Operation {

  val default = Operation(Source.default, Source.default, Source.default, OperationType.nothing)

  def noMatch = {
    val operation = Wire(new Operation())
    operation.opType := OperationType.noMatch.asUInt
    operation.src1   := DontCare
    operation.src2   := DontCare
    operation.dst    := DontCare
    operation
  }

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

class OeprationGroup() extends Bundle {}

object InstructionType extends ChiselEnum {
  val rType, iType, sType, uType, jType, bType, noType = Value
}

object InstructionResType extends ChiselEnum {
  val further, noMatch, ok, other = Value
}

class Instruction() extends Bundle {
  val status          = UInt(InstructionResType.getWidth.W)
  val instructionType = UInt(InstructionType.getWidth.W)
  val op              = Vec(2, Operation())
}

object Instruction {

  def further = {
    val inst = Wire(new Instruction())
    inst.status          := InstructionResType.further.asUInt
    inst.instructionType := DontCare
    inst.op              := DontCare
    inst
  }

  def noMatch = {
    val inst = Wire(new Instruction())
    inst.status          := InstructionResType.noMatch.asUInt
    inst.instructionType := DontCare
    inst.op(0)           := Operation.noMatch
    inst.op(1)           := Operation.noMatch
    inst
  }

  def apply(instType: InstructionType.Type, op: Operation) = {
    val inst = Wire(new Instruction())
    inst.status          := InstructionResType.ok.asUInt
    inst.op(0)           := op
    inst.op(1)           := Operation.default
    inst.instructionType := instType.asUInt
    inst
  }

  def apply(instType: InstructionType.Type, op1: Operation, op2: Operation) = {
    val inst = Wire(new Instruction())
    inst.status          := InstructionResType.ok.asUInt
    inst.op(0)           := op1
    inst.op(1)           := op2
    inst.instructionType := instType.asUInt
    inst
  }

  def apply() = new Instruction

}
