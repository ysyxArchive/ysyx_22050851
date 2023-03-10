import chisel3._
import chisel3.experimental.ChiselEnum
import chisel3.util._
import utils._
import scala.language.postfixOps
import org.apache.commons.lang3.ObjectUtils
import decode._
import org.joda.time.field.SkipUndoDateTimeField
import execute.ALUUtils

class InstructionDecodeUnit extends Module {
  val io = IO(new Bundle {
    val enable = Input(Bool())
    val inst   = Input(UInt(32.W))
    val ready  = Output(Bool())
  })
  val output = IO(Decoupled(Vec(2, Operation())))

  val resultValid = RegInit(false.B)

  io.ready     := output.ready
  output.valid := !output.ready && resultValid
  output.bits  := DontCare

  val rs1    = io.inst(19, 15)
  val rs2    = io.inst(24, 20)
  val rd     = io.inst(11, 7)
  val opcode = io.inst(6, 0)
  val funct3 = io.inst(14, 12)
  val funct7 = io.inst(31, 25)
  val immI   = Utils.signalExtend(io.inst(31, 20), 12)
  val immS   = Cat(io.inst(31, 25), io.inst(11, 7))
  val immU   = Cat(Utils.signalExtend(io.inst(31, 12), 20), 0.U(12.W))
  val immB   = Cat(Utils.signalExtend(io.inst(31), 1), io.inst(7), io.inst(30, 25), io.inst(11, 8), 0.U(1.W))
  val immJ = Utils.signalExtend(
    Cat(io.inst(31), io.inst(19, 12), io.inst(20), io.inst(30, 21), 0.U(1.W)),
    20
  );

  val result = MuxLookup(
    opcode,
    Instruction.further,
    Seq(
      "b0010111".U -> Instruction( // auipc
        InstructionType.uType,
        Operation(Source.pc, Source.imm(immU), Source.reg(rd), OperationType.add)
      ),
      "b0110111".U -> Instruction( // lui
        InstructionType.uType,
        Operation(Source.imm(immU), Source.default, Source.reg(rd), OperationType.move)
      ),
      "b1101111".U -> Instruction( // jal
        InstructionType.jType,
        Operation(Source.pc, Source.imm(immJ), Source.pc, OperationType.add),
        Operation(Source.npc, Source.default, Source.reg(rd), OperationType.move)
      )
    )
  )
  val result2 = MuxLookup(
    Cat(funct3, opcode),
    Instruction.further,
    Seq(
      "b0000000011".U -> Instruction( // lb
        InstructionType.iType,
        Operation(Source.reg(rs1), Source.imm(immI), Source.none, OperationType.add),
        Operation(Source.alu, Source.reg(rd), Source.imm(1.U), OperationType.loadmemS)
      ),
      "b0001100111".U -> Instruction( // jalr
        InstructionType.iType,
        Operation(Source.reg(rs1), Source.imm(immI), Source.pc, OperationType.add),
        Operation(Source.npc, Source.default, Source.reg(rd), OperationType.move)
      ),
      "b0010000011".U -> Instruction( // lh
        InstructionType.iType,
        Operation(Source.reg(rs1), Source.imm(immI), Source.none, OperationType.add),
        Operation(Source.alu, Source.reg(rd), Source.imm(2.U), OperationType.loadmemS)
      ),
      "b0100000011".U -> Instruction( // lw
        InstructionType.iType,
        Operation(Source.reg(rs1), Source.imm(immI), Source.none, OperationType.add),
        Operation(Source.alu, Source.reg(rd), Source.imm(4.U), OperationType.loadmemS)
      ),
      "b0110000011".U -> Instruction( // ld
        InstructionType.iType,
        Operation(Source.reg(rs1), Source.imm(immI), Source.none, OperationType.add),
        Operation(Source.alu, Source.reg(rd), Source.imm(8.U), OperationType.loadmemU)
      ),
      "b0110000011".U -> Instruction( // lbu
        InstructionType.iType,
        Operation(Source.reg(rs1), Source.imm(immI), Source.none, OperationType.add),
        Operation(Source.alu, Source.reg(rd), Source.imm(1.U), OperationType.loadmemU)
      ),
      "b0110000011".U -> Instruction( // lhu
        InstructionType.iType,
        Operation(Source.reg(rs1), Source.imm(immI), Source.none, OperationType.add),
        Operation(Source.alu, Source.reg(rd), Source.imm(2.U), OperationType.loadmemU)
      ),
      "b0110000011".U -> Instruction( // lwu
        InstructionType.iType,
        Operation(Source.reg(rs1), Source.imm(immI), Source.none, OperationType.add),
        Operation(Source.alu, Source.reg(rd), Source.imm(4.U), OperationType.loadmemU)
      ),
      "b0100100011".U -> Instruction( // sb
        InstructionType.sType,
        Operation(Source.reg(rs1), Source.imm(immS), Source.none, OperationType.add),
        Operation(Source.alu, Source.reg(rs2), Source.imm(1.U), OperationType.savemem)
      ),
      "b0100100011".U -> Instruction( // sh
        InstructionType.sType,
        Operation(Source.reg(rs1), Source.imm(immS), Source.none, OperationType.add),
        Operation(Source.alu, Source.reg(rs2), Source.imm(2.U), OperationType.savemem)
      ),
      "b0100100011".U -> Instruction( // sw
        InstructionType.sType,
        Operation(Source.reg(rs1), Source.imm(immS), Source.none, OperationType.add),
        Operation(Source.alu, Source.reg(rs2), Source.imm(4.U), OperationType.savemem)
      ),
      "b0110100011".U -> Instruction( // sd
        InstructionType.sType,
        Operation(Source.reg(rs1), Source.imm(immS), Source.none, OperationType.add),
        Operation(Source.alu, Source.reg(rs2), Source.imm(8.U), OperationType.savemem)
      ),
      "b0000010011".U -> Instruction( // addi
        InstructionType.iType,
        Operation(Source.reg(rs1), Source.imm(immI), Source.reg(rd), OperationType.add)
      ),
      "b0000011011".U -> Instruction( // addiw
        InstructionType.iType,
        Operation(Source.regLow(rs1), Source.imm(immI), Source.regLow(rd), OperationType.add)
      ),
      "b0001100011".U -> Instruction( // beq
        InstructionType.bType,
        Operation(Source.reg(rs1), Source.reg(rs2), Source.none, OperationType.sub),
        Operation(Source.imm(immB), Source.pc, Source.pc(ALUUtils.isZero), OperationType.moveBranch)
      ),
      "b0011100011".U -> Instruction( // bne
        InstructionType.bType,
        Operation(Source.reg(rs1), Source.reg(rs2), Source.none, OperationType.sub),
        Operation(Source.imm(immB), Source.pc, Source.pc(ALUUtils.notZero), OperationType.moveBranch)
      )
    )
  )
  val result3 = MuxLookup(
    Cat(funct7, funct3, opcode),
    Instruction.further,
    Seq(
      "b00000000000111011".U -> Instruction( // addw
        InstructionType.rType,
        Operation(Source.regLow(rs1), Source.regLow(rs2), Source.reg(rd), OperationType.add)
      ),
      "b01000000000111011".U -> Instruction( // subw
        InstructionType.rType,
        Operation(Source.regLow(rs1), Source.regLow(rs2), Source.reg(rd), OperationType.sub)
      )
      // "b00000001100110011".U -> Instruction( // or
      //   Instruction.rType,
      //   Operation(Source.reg(rs1), Source.reg(rs2), Source.temp, OperationType.or)
      // )
      //     , Reg(dest) = src1 | src2);
      // INSTPAT("0000000 ????? ????? 111 ????? 01100 11", and    , R, Reg(dest) = src1 & src2);
      // INSTPAT("0000000 ????? ????? 100 ????? 01100 11", xor    , R, Reg(dest) = src1 ^ src2);
    )
  )
  val result4 = MuxLookup(
    io.inst,
    Instruction.noMatch,
    Seq(
      "b00000000000100000000000001110011".U -> Instruction(
        InstructionType.iType,
        Operation(Source.imm(0.U), Source.imm(3.U), Source.pc, OperationType.halt)
      )
    )
  )

  val finalresult = Mux(
    result.status === InstructionResType.ok.asUInt,
    result,
    Mux(
      result2.status === InstructionResType.ok.asUInt,
      result2,
      Mux(
        result3.status === InstructionResType.ok.asUInt,
        result3,
        Mux(
          result4.status === InstructionResType.ok.asUInt,
          result4,
          Instruction.noMatch
        )
      )
    )
  )
  when(io.enable) {
    output.enq(finalresult.op)
  }

}
