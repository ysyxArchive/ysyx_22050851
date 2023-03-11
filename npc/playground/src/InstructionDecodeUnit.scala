import chisel3._
import chisel3.experimental.ChiselEnum
import chisel3.util._
import utils._
import scala.language.postfixOps
import org.apache.commons.lang3.ObjectUtils
import decode._
import org.joda.time.field.SkipUndoDateTimeField
import execute._

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
        Operation(Source.pc, Source.imm(immU), Source.alu(ALUType.add), OperationType.alu),
        Operation(Source.alu, Source.default, Source.reg(rd), OperationType.move)
      ),
      "b0110111".U -> Instruction( // lui
        InstructionType.uType,
        Operation(Source.imm(immU), Source.default, Source.reg(rd), OperationType.move)
      ),
      "b1101111".U -> Instruction( // jal
        InstructionType.jType,
        Operation(Source.pc, Source.imm(immJ), Source.pc, OperationType.updatePC),
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
        Operation(Source.reg(rs1), Source.imm(immI), Source.alu(ALUType.add), OperationType.alu),
        Operation(Source.alu, Source.reg(rd), Source.imm(1.U), OperationType.loadmemS)
      ),
      "b0001100111".U -> Instruction( // jalr
        InstructionType.iType,
        Operation(Source.reg(rs1), Source.imm(immI), Source.pc, OperationType.updatePC),
        Operation(Source.npc, Source.default, Source.reg(rd), OperationType.move)
      ),
      "b0010000011".U -> Instruction( // lh
        InstructionType.iType,
        Operation(Source.reg(rs1), Source.imm(immI), Source.alu(ALUType.add), OperationType.alu),
        Operation(Source.alu, Source.imm(2.U), Source.reg(rd), OperationType.loadmemS)
      ),
      "b0100000011".U -> Instruction( // lw
        InstructionType.iType,
        Operation(Source.reg(rs1), Source.imm(immI), Source.alu(ALUType.add), OperationType.alu),
        Operation(Source.alu, Source.imm(4.U), Source.reg(rd), OperationType.loadmemS)
      ),
      "b0110000011".U -> Instruction( // ld
        InstructionType.iType,
        Operation(Source.reg(rs1), Source.imm(immI), Source.alu(ALUType.add), OperationType.alu),
        Operation(Source.alu, Source.imm(8.U), Source.reg(rd), OperationType.loadmemU)
      ),
      "b1000000011".U -> Instruction( // lbu
        InstructionType.iType,
        Operation(Source.reg(rs1), Source.imm(immI), Source.alu(ALUType.add), OperationType.alu),
        Operation(Source.alu, Source.imm(1.U), Source.reg(rd), OperationType.loadmemU)
      ),
      "b1010000011".U -> Instruction( // lhu
        InstructionType.iType,
        Operation(Source.reg(rs1), Source.imm(immI), Source.alu(ALUType.add), OperationType.alu),
        Operation(Source.alu, Source.imm(2.U), Source.reg(rd), OperationType.loadmemU)
      ),
      "b1100000011".U -> Instruction( // lwu
        InstructionType.iType,
        Operation(Source.reg(rs1), Source.imm(immI), Source.alu(ALUType.add), OperationType.alu),
        Operation(Source.alu, Source.imm(4.U), Source.reg(rd), OperationType.loadmemU)
      ),
      "b0000100011".U -> Instruction( // sb
        InstructionType.sType,
        Operation(Source.reg(rs1), Source.imm(immS), Source.alu(ALUType.add), OperationType.alu),
        Operation(Source.alu, Source.reg(rs2), Source.imm(1.U), OperationType.savemem)
      ),
      "b0010100011".U -> Instruction( // sh
        InstructionType.sType,
        Operation(Source.reg(rs1), Source.imm(immS), Source.alu(ALUType.add), OperationType.alu),
        Operation(Source.alu, Source.reg(rs2), Source.imm(2.U), OperationType.savemem)
      ),
      "b0100100011".U -> Instruction( // sw
        InstructionType.sType,
        Operation(Source.reg(rs1), Source.imm(immS), Source.alu(ALUType.add), OperationType.alu),
        Operation(Source.alu, Source.reg(rs2), Source.imm(4.U), OperationType.savemem)
      ),
      "b0110100011".U -> Instruction( // sd
        InstructionType.sType,
        Operation(Source.reg(rs1), Source.imm(immS), Source.alu(ALUType.add), OperationType.alu),
        Operation(Source.alu, Source.reg(rs2), Source.imm(8.U), OperationType.savemem)
      ),
      "b0000010011".U -> Instruction( // addi
        InstructionType.iType,
        Operation(Source.reg(rs1), Source.imm(immI), Source.alu(ALUType.add), OperationType.alu),
        Operation(Source.alu, Source.default, Source.reg(rd), OperationType.move)
      ),
      "b0000011011".U -> Instruction( // addiw
        InstructionType.iType,
        Operation(Source.regLow(rs1), Source.imm(immI), Source.alu(ALUType.add), OperationType.alu),
        Operation(Source.alu, Source.default, Source.regLow(rd), OperationType.move)
      ),
      "b0001100011".U -> Instruction( // beq
        InstructionType.bType,
        Operation(Source.reg(rs1), Source.reg(rs2), Source.alu(ALUType.sub), OperationType.alu),
        Operation(Source.imm(immB), Source.pc, Source.pc(ALUUtils.isZero), OperationType.updatePC)
      ),
      "b0011100011".U -> Instruction( // bne
        InstructionType.bType,
        Operation(Source.reg(rs1), Source.reg(rs2), Source.alu(ALUType.sub), OperationType.alu),
        Operation(Source.imm(immB), Source.pc, Source.pc(ALUUtils.notZero), OperationType.updatePC)
      ),
      "b1001100011".U -> Instruction( // blt
        InstructionType.bType,
        Operation(Source.reg(rs1), Source.reg(rs2), Source.alu(ALUType.sub), OperationType.alu),
        Operation(Source.imm(immB), Source.pc, Source.pc(ALUUtils.isNegative), OperationType.updatePC)
      ),
      "b1011100011".U -> Instruction( // bge
        InstructionType.bType,
        Operation(Source.reg(rs1), Source.reg(rs2), Source.alu(ALUType.sub), OperationType.alu),
        Operation(
          Source.imm(immB),
          Source.pc,
          Source.pc(ALUUtils.isZero | ALUUtils.notNegative),
          OperationType.updatePC
        )
      ),
      "b1101100011".U -> Instruction( // bltu
        InstructionType.bType,
        Operation(Source.reg(rs1), Source.reg(rs2), Source.alu(ALUType.sub), OperationType.alu),
        Operation(Source.imm(immB), Source.pc, Source.pc(ALUUtils.isNegative), OperationType.updatePC)
      ),
      "b1111100011".U -> Instruction( // bgeu
        InstructionType.bType,
        Operation(Source.reg(rs1), Source.reg(rs2), Source.alu(ALUType.sub), OperationType.alu),
        Operation(
          Source.imm(immB),
          Source.pc,
          Source.pc(ALUUtils.isZero | ALUUtils.notNegative),
          OperationType.updatePC
        )
      ),
      "b1100010011".U -> Instruction( // ori
        InstructionType.iType,
        Operation(Source.reg(rs1), Source.imm(immI), Source.alu(ALUType.or), OperationType.alu),
        Operation(Source.alu, Source.default, Source.reg(rd), OperationType.move)
      ),
      "b1110010011".U -> Instruction( // andi
        InstructionType.iType,
        Operation(Source.reg(rs1), Source.imm(immI), Source.alu(ALUType.and), OperationType.alu),
        Operation(Source.alu, Source.default, Source.reg(rd), OperationType.move)
      ),
      "b1000010011".U -> Instruction( // xori
        InstructionType.iType,
        Operation(Source.reg(rs1), Source.imm(immI), Source.alu(ALUType.xor), OperationType.alu),
        Operation(Source.alu, Source.default, Source.reg(rd), OperationType.move)
      ),
      "b0100010011".U -> Instruction( // slti
        InstructionType.iType,
        Operation(Source.reg(rs1), Source.imm(immI), Source.alu(ALUType.sub), OperationType.alu),
        Operation(Source.aluSign(ALUSignalType.isZero), Source.default, Source.reg(rd), OperationType.move)
      ),
      "b0110010011".U -> Instruction( // sltiu
        InstructionType.iType,
        Operation(Source.reg(rs1), Source.imm(immI), Source.alu(ALUType.sub), OperationType.alu),
        Operation(Source.aluSign(ALUSignalType.isZero), Source.default, Source.reg(rd), OperationType.move)
      )
    )
  )

  val result3 = MuxLookup(
    Cat(funct7(6, 1), funct3, opcode),
    Instruction.further,
    Seq(
      "b0000000010010011".U -> Instruction( // slli
        InstructionType.iType,
        Operation(Source.regLow(rs1), Source.imm(immI), Source.alu(ALUType.shiftLeft), OperationType.alu),
        Operation(Source.alu, Source.default, Source.reg(rd), OperationType.move)
      ),
      "b0000001010010011".U -> Instruction( // srli
        InstructionType.iType,
        Operation(Source.regLow(rs1), Source.imm(immI), Source.alu(ALUType.shiftRightLogic), OperationType.alu),
        Operation(Source.alu, Source.default, Source.reg(rd), OperationType.move)
      ),
      "b0100001010010011".U -> Instruction( // srai
        InstructionType.iType,
        Operation(Source.regLow(rs1), Source.imm(immI), Source.alu(ALUType.shiftRightArth), OperationType.alu),
        Operation(Source.alu, Source.default, Source.reg(rd), OperationType.move)
      )
    )
  )

  val result4 = MuxLookup(
    Cat(funct7, funct3, opcode),
    Instruction.further,
    Seq(
      "b00000000000111011".U -> Instruction( // addw
        InstructionType.rType,
        Operation(Source.regLow(rs1), Source.regLow(rs2), Source.alu(ALUType.add), OperationType.alu),
        Operation(Source.alu, Source.default, Source.reg(rd), OperationType.move)
      ),
      "b01000000000111011".U -> Instruction( // subw
        InstructionType.rType,
        Operation(Source.regLow(rs1), Source.regLow(rs2), Source.alu(ALUType.sub), OperationType.alu),
        Operation(Source.alu, Source.default, Source.reg(rd), OperationType.move)
      ),
      "b00000000000110011".U -> Instruction( // add
        InstructionType.rType,
        Operation(Source.reg(rs1), Source.reg(rs2), Source.alu(ALUType.add), OperationType.alu),
        Operation(Source.alu, Source.default, Source.reg(rd), OperationType.move)
      ),
      "b01000000000110011".U -> Instruction( // sub
        InstructionType.rType,
        Operation(Source.reg(rs1), Source.reg(rs2), Source.alu(ALUType.sub), OperationType.alu),
        Operation(Source.alu, Source.default, Source.reg(rd), OperationType.move)
      ),
      "b00000000100110011".U -> Instruction( // slt
        InstructionType.rType,
        Operation(Source.reg(rs1), Source.reg(rs2), Source.alu(ALUType.sub), OperationType.alu),
        Operation(Source.aluSign(ALUSignalType.isNegative), Source.default, Source.reg(rd), OperationType.move)
      ),
      "b00000000110110011".U -> Instruction( // sltu
        InstructionType.rType,
        Operation(Source.reg(rs1), Source.reg(rs2), Source.alu(ALUType.sub), OperationType.alu),
        Operation(Source.aluSign(ALUSignalType.isNegative), Source.default, Source.reg(rd), OperationType.move)
      ),
      "b00000001100110011".U -> Instruction( // or
        InstructionType.rType,
        Operation(Source.reg(rs1), Source.reg(rs2), Source.alu(ALUType.or), OperationType.alu),
        Operation(Source.alu, Source.default, Source.reg(rd), OperationType.move)
      ),
      "b00000001110110011".U -> Instruction( // and
        InstructionType.rType,
        Operation(Source.reg(rs1), Source.reg(rs2), Source.alu(ALUType.and), OperationType.alu),
        Operation(Source.alu, Source.default, Source.reg(rd), OperationType.move)
      ),
      "b00000001000110011".U -> Instruction( // xor
        InstructionType.rType,
        Operation(Source.reg(rs1), Source.reg(rs2), Source.alu(ALUType.xor), OperationType.alu),
        Operation(Source.alu, Source.default, Source.reg(rd), OperationType.move)
      ),
      "b00000010000110011".U -> Instruction( // mul
        InstructionType.rType,
        Operation(Source.reg(rs1), Source.reg(rs2), Source.temp, OperationType.mul),
        Operation(Source.temp, Source.default, Source.reg(rd), OperationType.move)
      ),
      "b00000010000111011".U -> Instruction( // mulw
        InstructionType.rType,
        Operation(Source.regLow(rs1), Source.regLow(rs2), Source.temp, OperationType.mul),
        Operation(Source.temp, Source.default, Source.regLow(rd), OperationType.move)
      ),
      "b00000011000110011".U -> Instruction( // div
        InstructionType.rType,
        Operation(Source.reg(rs1), Source.reg(rs2), Source.temp, OperationType.divS),
        Operation(Source.temp, Source.default, Source.reg(rd), OperationType.move)
      ),
      "b00000011010110011".U -> Instruction( // divu
        InstructionType.rType,
        Operation(Source.reg(rs1), Source.reg(rs2), Source.temp, OperationType.div),
        Operation(Source.temp, Source.default, Source.reg(rd), OperationType.move)
      ),
      "b00000011100110011".U -> Instruction( // rem
        InstructionType.rType,
        Operation(Source.reg(rs1), Source.reg(rs2), Source.temp, OperationType.remS),
        Operation(Source.temp, Source.default, Source.reg(rd), OperationType.move)
      ),
      "b00000011110110011".U -> Instruction( // remu
        InstructionType.rType,
        Operation(Source.reg(rs1), Source.reg(rs2), Source.temp, OperationType.rem),
        Operation(Source.temp, Source.default, Source.reg(rd), OperationType.move)
      ),
      "b00000011100111011".U -> Instruction( // remw
        InstructionType.rType,
        Operation(Source.regLow(rs1), Source.regLow(rs2), Source.temp, OperationType.remS),
        Operation(Source.temp, Source.default, Source.regLow(rd), OperationType.move)
      ),
      "b00000000010111011".U -> Instruction( // sllw
        InstructionType.rType,
        Operation(Source.regLow(rs1), Source.regLow(rs2), Source.alu(ALUType.shiftLeft), OperationType.alu),
        Operation(Source.alu, Source.default, Source.regLow(rd), OperationType.move)
      ),
      "b00000001010111011".U -> Instruction( // srlw
        InstructionType.rType,
        Operation(Source.regLow(rs1), Source.regLow(rs2), Source.alu(ALUType.shiftRightLogic), OperationType.alu),
        Operation(Source.alu, Source.default, Source.regLow(rd), OperationType.move)
      ),
      "b01000001010111011".U -> Instruction( // sraw
        InstructionType.rType,
        Operation(Source.regLow(rs1), Source.regLow(rs2), Source.alu(ALUType.shiftRightArth), OperationType.alu),
        Operation(Source.alu, Source.default, Source.regLow(rd), OperationType.move)
      )
    )
  )
// INSTPAT("0000000 ????? ????? 001 ????? 01110 11", sllw   , R, Reg(dest) = SEXT(BITS(src1, 31, 0) << BITS(src2, 4, 0), 32));
//   INSTPAT("0000000 ????? ????? 101 ????? 01110 11", srlw   , R, Reg(dest) = SEXT(BITS(src1, 31, 0) >> BITS(src2, 4, 0), 32));
//   INSTPAT("0100000 ????? ????? 101 ????? 01110 11", sraw   , R, Reg(dest) = SEXT((sword_t)SEXT(BITS(src1, 31, 0), 32) >> BITS(src2, 4, 0), 32));

  val result5 = MuxLookup(
    io.inst,
    Instruction.noMatch,
    Seq(
      "b00000000000100000000000001110011".U -> Instruction(
        InstructionType.iType,
        Operation(Source.imm(0.U), Source.imm(3.U), Source.pc, OperationType.halt)
      )
    )
  )

  val resultSeq = Seq(result, result2, result3, result4, result5)
  val finalresult = MuxLookup(
    InstructionResType.ok.asUInt,
    Instruction.noMatch,
    resultSeq.map(res => res.status -> res)
  )

  when(io.enable) {
    output.enq(finalresult.op)
  }

}
