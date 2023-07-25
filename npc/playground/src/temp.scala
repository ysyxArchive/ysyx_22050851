
// AUTO GENERATED CODE DO NOT EDIT
import chisel3._
import chisel3.util.BitPat
import chisel3.util.experimental.decode._
import chisel3.experimental.ChiselEnum
object src1cast extends ChiselEnum {
	val no = Value(0.U)
	val yes = Value(1.U)
}
object src2cast extends ChiselEnum {
	val no = Value(0.U)
	val yes = Value(1.U)
}
object alumux1 extends ChiselEnum {
	val src1 = Value(0.U)
	val pc = Value(1.U)
	val zero = Value(2.U)
}
object alumux2 extends ChiselEnum {
	val src2 = Value(0.U)
	val imm = Value(1.U)
}
object alumode extends ChiselEnum {
	val add = Value(0.U)
	val and = Value(1.U)
	val sub = Value(2.U)
	val div = Value(3.U)
	val divu = Value(4.U)
	val mul = Value(5.U)
	val or = Value(6.U)
	val rem = Value(7.U)
	val remu = Value(8.U)
	val ll = Value(9.U)
	val ra = Value(10.U)
	val rl = Value(11.U)
	val xor = Value(12.U)
}
object regw extends ChiselEnum {
	val yes = Value(0.U)
	val no = Value(1.U)
}
object regwmux extends ChiselEnum {
	val alu = Value(0.U)
	val snpc = Value(1.U)
	val mem = Value(2.U)
	val aluneg = Value(3.U)
}
object regwsignextend extends ChiselEnum {
	val no = Value(0.U)
	val yes = Value(1.U)
}
object memmode extends ChiselEnum {
	val no = Value(0.U)
	val read = Value(1.U)
	val readu = Value(2.U)
	val write = Value(3.U)
}
object memlen extends ChiselEnum {
	val one = Value(0.U)
	val eight = Value(1.U)
	val two = Value(2.U)
	val four = Value(3.U)
}
object pcaddrsrc extends ChiselEnum {
	val zero = Value(0.U)
	val aluzero = Value(1.U)
	val alunotneg = Value(2.U)
	val aluneg = Value(3.U)
	val alunotzero = Value(4.U)
}
object pcsrc extends ChiselEnum {
	val imm = Value(0.U)
	val src1 = Value(1.U)
}
class DecodeOut extends Bundle {
	val src1cast = Output(UInt(1.W))
	val src2cast = Output(UInt(1.W))
	val alumux1 = Output(UInt(2.W))
	val alumux2 = Output(UInt(1.W))
	val alumode = Output(UInt(4.W))
	val regw = Output(UInt(1.W))
	val regwmux = Output(UInt(2.W))
	val regwsignextend = Output(UInt(1.W))
	val memmode = Output(UInt(2.W))
	val memlen = Output(UInt(2.W))
	val pcaddrsrc = Output(UInt(3.W))
	val pcsrc = Output(UInt(1.W))
}

class InstDecoder extends Module {
  val table = TruthTable(
    Map(
		BitPat("b0000000 ????? ????? 000 ????? 01100 11") -> BitPat("b0 0 00 0 0000 0 00 0 00 ?? 000 ?"), // add
		BitPat("b??????? ????? ????? 000 ????? 00100 11") -> BitPat("b0 0 00 1 0000 0 00 0 00 ?? 000 ?"), // addi
		BitPat("b??????? ????? ????? 000 ????? 00110 11") -> BitPat("b0 0 00 1 0000 0 00 1 00 ?? 000 ?"), // addiw
		BitPat("b0000000 ????? ????? 000 ????? 01110 11") -> BitPat("b1 1 00 0 0000 0 00 1 00 ?? 000 ?"), // addw
		BitPat("b0000000 ????? ????? 111 ????? 01100 11") -> BitPat("b0 0 00 0 0001 0 00 0 00 ?? 000 ?"), // and
		BitPat("b??????? ????? ????? 111 ????? 00100 11") -> BitPat("b0 0 00 1 0001 0 00 0 00 ?? 000 ?"), // andi
		BitPat("b??????? ????? ????? ??? ????? 00101 11") -> BitPat("b0 0 01 1 0000 0 00 0 00 ?? 000 ?"), // auipc
		BitPat("b??????? ????? ????? 000 ????? 11000 11") -> BitPat("b0 0 00 0 0010 1 ?? 0 00 ?? 001 0"), // beq
		BitPat("b??????? ????? ????? 101 ????? 11000 11") -> BitPat("b0 0 00 0 0010 1 ?? 0 00 ?? 010 0"), // bge
		BitPat("b??????? ????? ????? 111 ????? 11000 11") -> BitPat("b0 0 00 0 0010 1 ?? 0 00 ?? 010 0"), // bgeu
		BitPat("b??????? ????? ????? 100 ????? 11000 11") -> BitPat("b0 0 00 0 0010 1 ?? 0 00 ?? 011 0"), // blt
		BitPat("b??????? ????? ????? 110 ????? 11000 11") -> BitPat("b0 0 00 0 0010 1 ?? 0 00 ?? 011 0"), // bltu
		BitPat("b??????? ????? ????? 001 ????? 11000 11") -> BitPat("b0 0 00 0 0010 1 ?? 0 00 ?? 100 0"), // bne
		BitPat("b0000001 ????? ????? 100 ????? 01100 11") -> BitPat("b0 0 00 0 0011 0 00 0 00 ?? 000 ?"), // div
		BitPat("b0000001 ????? ????? 101 ????? 01100 11") -> BitPat("b0 0 00 0 0100 0 00 0 00 ?? 000 ?"), // divu
		BitPat("b0000001 ????? ????? 101 ????? 01110 11") -> BitPat("b1 1 00 0 0100 0 00 1 00 ?? 000 ?"), // divuw
		BitPat("b0000001 ????? ????? 100 ????? 01110 11") -> BitPat("b1 1 00 0 0011 0 00 1 00 ?? 000 ?"), // divw
		BitPat("b??????? ????? ????? ??? ????? 11011 11") -> BitPat("b0 0 ?? ? ???? 0 01 0 00 ?? 000 0"), // jal
		BitPat("b??????? ????? ????? 000 ????? 11001 11") -> BitPat("b0 0 ?? ? ???? 0 01 0 00 ?? 000 1"), // jalr
		BitPat("b??????? ????? ????? 000 ????? 00000 11") -> BitPat("b0 0 00 1 0000 0 10 0 01 00 000 ?"), // lb
		BitPat("b??????? ????? ????? 100 ????? 00000 11") -> BitPat("b0 0 00 1 0000 0 10 0 10 00 000 ?"), // lbu
		BitPat("b??????? ????? ????? 011 ????? 00000 11") -> BitPat("b0 0 00 1 0000 0 10 0 01 01 000 ?"), // ld
		BitPat("b??????? ????? ????? 001 ????? 00000 11") -> BitPat("b0 0 00 1 0000 0 10 0 01 10 000 ?"), // lh
		BitPat("b??????? ????? ????? 101 ????? 00000 11") -> BitPat("b0 0 00 1 0000 0 10 0 10 10 000 ?"), // lhu
		BitPat("b??????? ????? ????? ??? ????? 01101 11") -> BitPat("b0 0 10 1 0000 0 00 0 00 ?? 000 ?"), // lui
		BitPat("b??????? ????? ????? 010 ????? 00000 11") -> BitPat("b0 0 00 1 0000 0 10 0 01 11 000 ?"), // lw
		BitPat("b??????? ????? ????? 110 ????? 00000 11") -> BitPat("b0 0 00 1 0000 0 10 0 10 11 000 ?"), // lwu
		BitPat("b0000001 ????? ????? 000 ????? 01100 11") -> BitPat("b0 0 00 0 0101 0 00 0 00 ?? 000 ?"), // mul
		BitPat("b0000001 ????? ????? 000 ????? 01110 11") -> BitPat("b1 1 00 0 0101 0 00 1 00 ?? 000 ?"), // mulw
		BitPat("b0000000 ????? ????? 110 ????? 01100 11") -> BitPat("b0 0 00 0 0110 0 00 0 00 ?? 000 ?"), // or
		BitPat("b??????? ????? ????? 110 ????? 00100 11") -> BitPat("b0 0 00 1 0110 0 00 0 00 ?? 000 ?"), // ori
		BitPat("b0000001 ????? ????? 110 ????? 01100 11") -> BitPat("b0 0 00 0 0111 0 00 0 00 ?? 000 ?"), // rem
		BitPat("b0000001 ????? ????? 111 ????? 01100 11") -> BitPat("b0 0 00 0 1000 0 00 0 00 ?? 000 ?"), // remu
		BitPat("b0000001 ????? ????? 111 ????? 01110 11") -> BitPat("b1 1 00 0 1000 0 00 1 00 ?? 000 ?"), // remuw
		BitPat("b0000001 ????? ????? 110 ????? 01110 11") -> BitPat("b1 1 00 0 0111 0 00 0 00 ?? 000 ?"), // remw
		BitPat("b??????? ????? ????? 000 ????? 01000 11") -> BitPat("b0 0 00 1 0000 1 ?? 0 11 00 000 ?"), // sb
		BitPat("b??????? ????? ????? 011 ????? 01000 11") -> BitPat("b0 0 00 1 0000 1 ?? 0 11 01 000 ?"), // sd
		BitPat("b??????? ????? ????? 001 ????? 01000 11") -> BitPat("b0 0 00 1 0000 1 ?? 0 11 10 000 ?"), // sh
		BitPat("b0000000 ????? ????? 001 ????? 01100 11") -> BitPat("b0 0 00 0 1001 0 00 0 00 ?? 000 ?"), // sll
		BitPat("b000000? ????? ????? 001 ????? 00100 11") -> BitPat("b0 0 00 1 1001 0 00 0 00 ?? 000 ?"), // slli
		BitPat("b0000000 ????? ????? 001 ????? 00110 11") -> BitPat("b1 0 00 1 1001 0 00 1 00 ?? 000 ?"), // slliw
		BitPat("b0000000 ????? ????? 001 ????? 01110 11") -> BitPat("b1 0 00 0 1001 0 00 1 00 ?? 000 ?"), // sllw
		BitPat("b0000000 ????? ????? 010 ????? 01100 11") -> BitPat("b0 0 00 0 0010 0 11 1 00 ?? 000 ?"), // slt
		BitPat("b??????? ????? ????? 010 ????? 00100 11") -> BitPat("b0 0 00 1 0010 0 11 1 00 ?? 000 ?"), // slti
		BitPat("b??????? ????? ????? 011 ????? 00100 11") -> BitPat("b0 0 00 1 0010 0 11 1 00 ?? 000 ?"), // sltiu
		BitPat("b0000000 ????? ????? 011 ????? 01100 11") -> BitPat("b0 0 00 0 0010 0 11 1 00 ?? 000 ?"), // sltu
		BitPat("b0100000 ????? ????? 001 ????? 01100 11") -> BitPat("b0 0 00 0 1010 0 00 0 00 ?? 000 ?"), // sra
		BitPat("b010000? ????? ????? 101 ????? 00100 11") -> BitPat("b0 0 00 1 1010 0 00 0 00 ?? 000 ?"), // srai
		BitPat("b0100000 ????? ????? 101 ????? 00110 11") -> BitPat("b1 0 00 1 1010 0 00 1 00 ?? 000 ?"), // sraiw
		BitPat("b0100000 ????? ????? 101 ????? 01110 11") -> BitPat("b1 0 00 0 1010 0 00 1 00 ?? 000 ?"), // sraw
		BitPat("b0000000 ????? ????? 101 ????? 01100 11") -> BitPat("b0 0 00 0 1011 0 00 0 00 ?? 000 ?"), // srl
		BitPat("b000000? ????? ????? 101 ????? 00100 11") -> BitPat("b0 0 00 1 1011 0 00 0 00 ?? 000 ?"), // srli
		BitPat("b0000000 ????? ????? 101 ????? 00110 11") -> BitPat("b1 0 00 1 1011 0 00 1 00 ?? 000 ?"), // srliw
		BitPat("b0000000 ????? ????? 101 ????? 01110 11") -> BitPat("b1 0 00 0 1011 0 00 1 00 ?? 000 ?"), // srlw
		BitPat("b0100000 ????? ????? 000 ????? 01100 11") -> BitPat("b0 0 00 0 0010 0 00 0 00 ?? 000 ?"), // sub
		BitPat("b0100000 ????? ????? 000 ????? 01110 11") -> BitPat("b1 1 00 0 0010 0 00 1 00 ?? 000 ?"), // subw
		BitPat("b??????? ????? ????? 010 ????? 01000 11") -> BitPat("b0 0 00 1 0000 1 ?? 0 11 11 000 ?"), // sw
		BitPat("b0000000 ????? ????? 100 ????? 01100 11") -> BitPat("b0 0 00 0 1100 0 00 0 00 ?? 000 ?"), // xor
		BitPat("b??????? ????? ????? 100 ????? 00100 11") -> BitPat("b0 0 00 1 1100 0 00 0 00 ?? 000 ?"), // xori
		BitPat("b??????? ????? ????? 011 ????? 11100 11") -> BitPat("b0 0 ?? ? ???? 1 ?? 0 00 ?? 000 ?"), // csrrc
		BitPat("b??????? ????? ????? 111 ????? 11100 11") -> BitPat("b0 0 ?? ? ???? 1 ?? 0 00 ?? 000 ?"), // csrrci
		BitPat("b??????? ????? ????? 010 ????? 11100 11") -> BitPat("b0 0 ?? ? ???? 1 ?? 0 00 ?? 000 ?"), // csrrs
		BitPat("b??????? ????? ????? 110 ????? 11100 11") -> BitPat("b0 0 ?? ? ???? 1 ?? 0 00 ?? 000 ?"), // csrrsi
		BitPat("b??????? ????? ????? 001 ????? 11100 11") -> BitPat("b0 0 ?? ? ???? 1 ?? 0 00 ?? 000 ?"), // csrrw
		BitPat("b??????? ????? ????? 101 ????? 11100 11") -> BitPat("b0 0 ?? ? ???? 1 ?? 0 00 ?? 000 ?"), // csrrwi
		BitPat("b0000000 00001 00000 000 00000 11100 11") -> BitPat("b0 0 ?? ? ???? 1 ?? 0 00 ?? 000 ?"), // ebreak
		BitPat("b0000000 00000 00000 000 00000 11100 11") -> BitPat("b0 0 ?? ? ???? 1 ?? 0 00 ?? 000 ?"), // ecall
		BitPat("b??????? ????? ????? ??? ????? ????? ??") -> BitPat("b0 0 ?? ? ???? 1 ?? 0 00 ?? 000 ?"), // inv
		BitPat("b0011000 00010 00000 000 00000 11100 11") -> BitPat("b0 0 ?? ? ???? 1 ?? 0 00 ?? 000 ?"), // mret
	),
    BitPat("b0 0 ?? ? ???? 1 ?? 0 00 ?? 000 ?") // inv
  )
  val input  = IO(Input(UInt(32.W)))
  val output  = IO(new DecodeOut)
  val decodeOut = decoder(input, table)
  output.src1cast := decodeOut(0, 0)
  output.src2cast := decodeOut(1, 1)
  output.alumux1 := decodeOut(2, 3)
  output.alumux2 := decodeOut(4, 4)
  output.alumode := decodeOut(5, 8)
  output.regw := decodeOut(9, 9)
  output.regwmux := decodeOut(10, 11)
  output.regwsignextend := decodeOut(12, 12)
  output.memmode := decodeOut(13, 14)
  output.memlen := decodeOut(15, 16)
  output.pcaddrsrc := decodeOut(17, 19)
  output.pcsrc := decodeOut(20, 20)
}