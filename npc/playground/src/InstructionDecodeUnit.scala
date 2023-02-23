import chisel3._
import chisel3.experimental.ChiselEnum
import chisel3.util._

import scala.language.postfixOps
import org.apache.commons.lang3.ObjectUtils
import decode._
import firrtl.backends.experimental.smt.Signal

object Utils {
  def signalExtend(num: UInt, length: Int): UInt = {
    Cat(Fill(64 - length, num(length - 1, length - 1)), num)
  }
}

class InstructionDecodeUnit extends Module {
  val io = IO(new Bundle {
    val enable = Input(Bool())
    val inst   = Input(UInt(32.W))
    val ready  = Output(Bool())
  })
  val output = IO(Decoupled(Vec(2, Operation())))

  val debugp = IO(Output(UInt(32.W)))

  val resultValid = RegInit(false.B)

  io.ready     := output.ready
  output.valid := !output.ready && resultValid
  output.bits  := DontCare

  val rs1    = io.inst(19, 15)
  val rs2    = io.inst(24, 20)
  val rd     = io.inst(11, 7)
  val opcode = io.inst(6, 0)
  val funct3 = io.inst(14, 12)
  val immI   = io.inst(31, 20)
  val immS   = Cat(io.inst(31, 25), io.inst(11, 7))
  val immU   = Cat(io.inst(31, 12), 0.U(12.W))
  val immJ   = Utils.signalExtend(Cat(io.inst(31), io.inst(19, 12), io.inst(20), io.inst(30, 21), 0.U(1.W)), 20);

  val result = MuxLookup(
    opcode,
    Instruction.noMatch,
    Seq(
      "b0010011".U -> Instruction.further,
      "b0100011".U -> Instruction.further,
      "b1100111".U -> Instruction.further,
      "b1110011".U -> Instruction.further,
      "b1101111".U -> Instruction( // jal
        InstructionType.jType,
        Operation(
          Source(0.U, SourceType.npc),
          Source.default,
          Source(rd, SourceType.reg),
          OperationType.move
        ),
        Operation(
          Source.pc,
          Source(immJ, SourceType.imm),
          Source.pc,
          OperationType.add
        )
      ),
      "b0010111".U -> Instruction( // auipc
        InstructionType.uType,
        Operation(
          Source(0.U, SourceType.pc),
          Source(immU, SourceType.imm),
          Source(rd, SourceType.reg),
          OperationType.add
        )
      )
    )
  )
  val result2 = MuxLookup(
    Cat(funct3, opcode),
    Instruction.noMatch,
    Seq(
      "b0001110011".U -> Instruction.further,
      "b0001100111".U -> Instruction( //jalr
        InstructionType.iType,
        Operation(
          Source(0.U, SourceType.npc),
          Source.default,
          Source(rd, SourceType.reg),
          OperationType.move
        ),
        Operation(
          Source(rs1, SourceType.reg),
          Source(immI, SourceType.imm),
          Source.pc,
          OperationType.add
        )
      ),
      "b0110100011".U -> Instruction(
        InstructionType.iType, //fixfixfixfixfixfixfixfixfixfix
        Operation( //fixfixfixfixfixfixfixfixfixfix
          Source(rs1, SourceType.reg), //fixfixfixfixfixfixfixfixfixfix
          Source(Utils.signalExtend(immI, 12), SourceType.imm), //fixfixfixfixfixfixfixfixfixfix
          Source(rd, SourceType.reg), //fixfixfixfixfixfixfixfixfixfix
          OperationType.add //fixfixfixfixfixfixfixfixfixfix
        )
      ),
      "b0000010011".U -> Instruction( // addi
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
    Instruction.noMatch,
    Seq(
      InstructionResType.ok.asUInt -> result,
      InstructionResType.further.asUInt -> MuxLookup(
        result2.status.asUInt,
        Instruction.noMatch,
        Seq(
          InstructionResType.ok.asUInt -> result2,
          InstructionResType.further.asUInt -> MuxLookup(
            result3.status.asUInt,
            Instruction.noMatch,
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
