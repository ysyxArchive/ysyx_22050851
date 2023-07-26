
// AUTO GENERATED CODE DO NOT EDIT
package decode

import chisel3._
import chisel3.util.BitPat
import chisel3.util.experimental.decode._
import chisel3.experimental.ChiselEnum
object InstType extends ChiselEnum {
	val  R = Value(0.U)
	val  I = Value(1.U)
	val  U = Value(2.U)
	val  B = Value(3.U)
	val  J = Value(4.U)
	val  S = Value(5.U)
	val  N = Value(6.U)
}
object SrcCast1 extends ChiselEnum {
	val no = Value(0.U)
	val yes = Value(1.U)
}
object SrcCast2 extends ChiselEnum {
	val no = Value(0.U)
	val yes = Value(1.U)
}
object AluMux1 extends ChiselEnum {
	val src1 = Value(0.U)
	val pc = Value(1.U)
	val zero = Value(2.U)
}
object AluMux2 extends ChiselEnum {
	val src2 = Value(0.U)
	val imm = Value(1.U)
}
object AluMode extends ChiselEnum {
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
	val rlw = Value(12.U)
	val xor = Value(13.U)
}
object RegWrite extends ChiselEnum {
	val yes = Value(0.U)
	val no = Value(1.U)
}
object RegWriteMux extends ChiselEnum {
	val alu = Value(0.U)
	val snpc = Value(1.U)
	val mem = Value(2.U)
	val aluneg = Value(3.U)
}
object RegWSEXT extends ChiselEnum {
	val no = Value(0.U)
	val yes = Value(1.U)
}
object MemMode extends ChiselEnum {
	val no = Value(0.U)
	val read = Value(1.U)
	val readu = Value(2.U)
	val write = Value(3.U)
}
object MemLen extends ChiselEnum {
	val one = Value(0.U)
	val eight = Value(1.U)
	val two = Value(2.U)
	val four = Value(3.U)
}
object PCAddrSrc extends ChiselEnum {
	val zero = Value(0.U)
	val aluzero = Value(1.U)
	val alunotneg = Value(2.U)
	val aluneg = Value(3.U)
	val alunotzero = Value(4.U)
	val one = Value(5.U)
}
object PcSrc extends ChiselEnum {
	val pc = Value(0.U)
	val src1 = Value(1.U)
}
object GoodTrap extends ChiselEnum {
	val no = Value(0.U)
	val yes = Value(1.U)
}
object BadTrap extends ChiselEnum {
	val no = Value(0.U)
	val yes = Value(1.U)
}
class DecodeControlOut extends Bundle {
	val insttype = Output(UInt(3.W))
	val srccast1 = Output(UInt(1.W))
	val srccast2 = Output(UInt(1.W))
	val alumux1 = Output(UInt(2.W))
	val alumux2 = Output(UInt(1.W))
	val alumode = Output(UInt(4.W))
	val regwrite = Output(UInt(1.W))
	val regwritemux = Output(UInt(2.W))
	val regwsext = Output(UInt(1.W))
	val memmode = Output(UInt(2.W))
	val memlen = Output(UInt(2.W))
	val pcaddrsrc = Output(UInt(3.W))
	val pcsrc = Output(UInt(1.W))
	val goodtrap = Output(UInt(1.W))
	val badtrap = Output(UInt(1.W))
}

object DecodeControlOut{
  def default() = {
    val defaultout = Wire(new DecodeControlOut);
	 defaultout.insttype := 0.U
	 defaultout.srccast1 := SrcCast1.no.asUInt
	 defaultout.srccast2 := SrcCast2.no.asUInt
	 defaultout.alumux1 := 0.U
	 defaultout.alumux2 := 0.U
	 defaultout.alumode := 0.U
	 defaultout.regwrite := RegWrite.no.asUInt
	 defaultout.regwritemux := 0.U
	 defaultout.regwsext := RegWSEXT.no.asUInt
	 defaultout.memmode := MemMode.no.asUInt
	 defaultout.memlen := 0.U
	 defaultout.pcaddrsrc := PCAddrSrc.zero.asUInt
	 defaultout.pcsrc := 0.U
	 defaultout.goodtrap := GoodTrap.no.asUInt
	 defaultout.badtrap := BadTrap.no.asUInt
defaultout
}
}

class InstContorlDecoder extends Module {
  val table = TruthTable(
    Map(
		  BitPat("b0000000 ????? ????? 000 ????? 01100 11") -> BitPat("b000 0 0 00 0 0000 0 00 0 00 ?? 000 ? 0 0"), // add
		  BitPat("b??????? ????? ????? 000 ????? 00100 11") -> BitPat("b001 0 0 00 1 0000 0 00 0 00 ?? 000 ? 0 0"), // addi
		  BitPat("b??????? ????? ????? 000 ????? 00110 11") -> BitPat("b001 0 0 00 1 0000 0 00 1 00 ?? 000 ? 0 0"), // addiw
		  BitPat("b0000000 ????? ????? 000 ????? 01110 11") -> BitPat("b000 1 1 00 0 0000 0 00 1 00 ?? 000 ? 0 0"), // addw
		  BitPat("b0000000 ????? ????? 111 ????? 01100 11") -> BitPat("b000 0 0 00 0 0001 0 00 0 00 ?? 000 ? 0 0"), // and
		  BitPat("b??????? ????? ????? 111 ????? 00100 11") -> BitPat("b001 0 0 00 1 0001 0 00 0 00 ?? 000 ? 0 0"), // andi
		  BitPat("b??????? ????? ????? ??? ????? 00101 11") -> BitPat("b010 0 0 01 1 0000 0 00 0 00 ?? 000 ? 0 0"), // auipc
		  BitPat("b??????? ????? ????? 000 ????? 11000 11") -> BitPat("b011 0 0 00 0 0010 1 ?? 0 00 ?? 001 0 0 0"), // beq
		  BitPat("b??????? ????? ????? 101 ????? 11000 11") -> BitPat("b011 0 0 00 0 0010 1 ?? 0 00 ?? 010 0 0 0"), // bge
		  BitPat("b??????? ????? ????? 111 ????? 11000 11") -> BitPat("b011 0 0 00 0 0010 1 ?? 0 00 ?? 010 0 0 0"), // bgeu
		  BitPat("b??????? ????? ????? 100 ????? 11000 11") -> BitPat("b011 0 0 00 0 0010 1 ?? 0 00 ?? 011 0 0 0"), // blt
		  BitPat("b??????? ????? ????? 110 ????? 11000 11") -> BitPat("b011 0 0 00 0 0010 1 ?? 0 00 ?? 011 0 0 0"), // bltu
		  BitPat("b??????? ????? ????? 001 ????? 11000 11") -> BitPat("b011 0 0 00 0 0010 1 ?? 0 00 ?? 100 0 0 0"), // bne
		  BitPat("b0000001 ????? ????? 100 ????? 01100 11") -> BitPat("b000 0 0 00 0 0011 0 00 0 00 ?? 000 ? 0 0"), // div
		  BitPat("b0000001 ????? ????? 101 ????? 01100 11") -> BitPat("b000 0 0 00 0 0100 0 00 0 00 ?? 000 ? 0 0"), // divu
		  BitPat("b0000001 ????? ????? 101 ????? 01110 11") -> BitPat("b000 1 1 00 0 0100 0 00 1 00 ?? 000 ? 0 0"), // divuw
		  BitPat("b0000001 ????? ????? 100 ????? 01110 11") -> BitPat("b000 1 1 00 0 0011 0 00 1 00 ?? 000 ? 0 0"), // divw
		  BitPat("b??????? ????? ????? ??? ????? 11011 11") -> BitPat("b100 0 0 ?? ? ???? 0 01 0 00 ?? 101 0 0 0"), // jal
		  BitPat("b??????? ????? ????? 000 ????? 11001 11") -> BitPat("b001 0 0 ?? ? ???? 0 01 0 00 ?? 101 1 0 0"), // jalr
		  BitPat("b??????? ????? ????? 000 ????? 00000 11") -> BitPat("b001 0 0 00 1 0000 0 10 0 01 00 000 ? 0 0"), // lb
		  BitPat("b??????? ????? ????? 100 ????? 00000 11") -> BitPat("b001 0 0 00 1 0000 0 10 0 10 00 000 ? 0 0"), // lbu
		  BitPat("b??????? ????? ????? 011 ????? 00000 11") -> BitPat("b001 0 0 00 1 0000 0 10 0 01 01 000 ? 0 0"), // ld
		  BitPat("b??????? ????? ????? 001 ????? 00000 11") -> BitPat("b001 0 0 00 1 0000 0 10 0 01 10 000 ? 0 0"), // lh
		  BitPat("b??????? ????? ????? 101 ????? 00000 11") -> BitPat("b001 0 0 00 1 0000 0 10 0 10 10 000 ? 0 0"), // lhu
		  BitPat("b??????? ????? ????? ??? ????? 01101 11") -> BitPat("b010 0 0 10 1 0000 0 00 0 00 ?? 000 ? 0 0"), // lui
		  BitPat("b??????? ????? ????? 010 ????? 00000 11") -> BitPat("b001 0 0 00 1 0000 0 10 0 01 11 000 ? 0 0"), // lw
		  BitPat("b??????? ????? ????? 110 ????? 00000 11") -> BitPat("b001 0 0 00 1 0000 0 10 0 10 11 000 ? 0 0"), // lwu
		  BitPat("b0000001 ????? ????? 000 ????? 01100 11") -> BitPat("b000 0 0 00 0 0101 0 00 0 00 ?? 000 ? 0 0"), // mul
		  BitPat("b0000001 ????? ????? 000 ????? 01110 11") -> BitPat("b000 1 1 00 0 0101 0 00 1 00 ?? 000 ? 0 0"), // mulw
		  BitPat("b0000000 ????? ????? 110 ????? 01100 11") -> BitPat("b000 0 0 00 0 0110 0 00 0 00 ?? 000 ? 0 0"), // or
		  BitPat("b??????? ????? ????? 110 ????? 00100 11") -> BitPat("b001 0 0 00 1 0110 0 00 0 00 ?? 000 ? 0 0"), // ori
		  BitPat("b0000001 ????? ????? 110 ????? 01100 11") -> BitPat("b000 0 0 00 0 0111 0 00 0 00 ?? 000 ? 0 0"), // rem
		  BitPat("b0000001 ????? ????? 111 ????? 01100 11") -> BitPat("b000 0 0 00 0 1000 0 00 0 00 ?? 000 ? 0 0"), // remu
		  BitPat("b0000001 ????? ????? 111 ????? 01110 11") -> BitPat("b000 1 1 00 0 1000 0 00 1 00 ?? 000 ? 0 0"), // remuw
		  BitPat("b0000001 ????? ????? 110 ????? 01110 11") -> BitPat("b000 1 1 00 0 0111 0 00 0 00 ?? 000 ? 0 0"), // remw
		  BitPat("b??????? ????? ????? 000 ????? 01000 11") -> BitPat("b101 0 0 00 1 0000 1 ?? 0 11 00 000 ? 0 0"), // sb
		  BitPat("b??????? ????? ????? 011 ????? 01000 11") -> BitPat("b101 0 0 00 1 0000 1 ?? 0 11 01 000 ? 0 0"), // sd
		  BitPat("b??????? ????? ????? 001 ????? 01000 11") -> BitPat("b101 0 0 00 1 0000 1 ?? 0 11 10 000 ? 0 0"), // sh
		  BitPat("b0000000 ????? ????? 001 ????? 01100 11") -> BitPat("b000 0 0 00 0 1001 0 00 0 00 ?? 000 ? 0 0"), // sll
		  BitPat("b000000? ????? ????? 001 ????? 00100 11") -> BitPat("b001 0 0 00 1 1001 0 00 0 00 ?? 000 ? 0 0"), // slli
		  BitPat("b0000000 ????? ????? 001 ????? 00110 11") -> BitPat("b001 1 0 00 1 1001 0 00 1 00 ?? 000 ? 0 0"), // slliw
		  BitPat("b0000000 ????? ????? 001 ????? 01110 11") -> BitPat("b000 1 0 00 0 1001 0 00 1 00 ?? 000 ? 0 0"), // sllw
		  BitPat("b0000000 ????? ????? 010 ????? 01100 11") -> BitPat("b000 0 0 00 0 0010 0 11 1 00 ?? 000 ? 0 0"), // slt
		  BitPat("b??????? ????? ????? 010 ????? 00100 11") -> BitPat("b001 0 0 00 1 0010 0 11 1 00 ?? 000 ? 0 0"), // slti
		  BitPat("b??????? ????? ????? 011 ????? 00100 11") -> BitPat("b001 0 0 00 1 0010 0 11 1 00 ?? 000 ? 0 0"), // sltiu
		  BitPat("b0000000 ????? ????? 011 ????? 01100 11") -> BitPat("b000 0 0 00 0 0010 0 11 1 00 ?? 000 ? 0 0"), // sltu
		  BitPat("b0100000 ????? ????? 001 ????? 01100 11") -> BitPat("b000 0 0 00 0 1010 0 00 0 00 ?? 000 ? 0 0"), // sra
		  BitPat("b010000? ????? ????? 101 ????? 00100 11") -> BitPat("b001 0 0 00 1 1010 0 00 0 00 ?? 000 ? 0 0"), // srai
		  BitPat("b0100000 ????? ????? 101 ????? 00110 11") -> BitPat("b001 1 0 00 1 1010 0 00 1 00 ?? 000 ? 0 0"), // sraiw
		  BitPat("b0100000 ????? ????? 101 ????? 01110 11") -> BitPat("b000 1 0 00 0 1010 0 00 1 00 ?? 000 ? 0 0"), // sraw
		  BitPat("b0000000 ????? ????? 101 ????? 01100 11") -> BitPat("b000 0 0 00 0 1011 0 00 0 00 ?? 000 ? 0 0"), // srl
		  BitPat("b000000? ????? ????? 101 ????? 00100 11") -> BitPat("b001 0 0 00 1 1011 0 00 0 00 ?? 000 ? 0 0"), // srli
		  BitPat("b0000000 ????? ????? 101 ????? 00110 11") -> BitPat("b001 1 0 00 1 1100 0 00 1 00 ?? 000 ? 0 0"), // srliw
		  BitPat("b0000000 ????? ????? 101 ????? 01110 11") -> BitPat("b000 1 0 00 0 1100 0 00 1 00 ?? 000 ? 0 0"), // srlw
		  BitPat("b0100000 ????? ????? 000 ????? 01100 11") -> BitPat("b000 0 0 00 0 0010 0 00 0 00 ?? 000 ? 0 0"), // sub
		  BitPat("b0100000 ????? ????? 000 ????? 01110 11") -> BitPat("b000 1 1 00 0 0010 0 00 1 00 ?? 000 ? 0 0"), // subw
		  BitPat("b??????? ????? ????? 010 ????? 01000 11") -> BitPat("b101 0 0 00 1 0000 1 ?? 0 11 11 000 ? 0 0"), // sw
		  BitPat("b0000000 ????? ????? 100 ????? 01100 11") -> BitPat("b000 0 0 00 0 1101 0 00 0 00 ?? 000 ? 0 0"), // xor
		  BitPat("b??????? ????? ????? 100 ????? 00100 11") -> BitPat("b001 0 0 00 1 1101 0 00 0 00 ?? 000 ? 0 0"), // xori
		  BitPat("b??????? ????? ????? 011 ????? 11100 11") -> BitPat("b001 0 0 ?? ? ???? 1 ?? 0 00 ?? 000 ? 0 1"), // csrrc
		  BitPat("b??????? ????? ????? 111 ????? 11100 11") -> BitPat("b001 0 0 ?? ? ???? 1 ?? 0 00 ?? 000 ? 0 1"), // csrrci
		  BitPat("b??????? ????? ????? 010 ????? 11100 11") -> BitPat("b001 0 0 ?? ? ???? 1 ?? 0 00 ?? 000 ? 0 1"), // csrrs
		  BitPat("b??????? ????? ????? 110 ????? 11100 11") -> BitPat("b001 0 0 ?? ? ???? 1 ?? 0 00 ?? 000 ? 0 1"), // csrrsi
		  BitPat("b??????? ????? ????? 001 ????? 11100 11") -> BitPat("b001 0 0 ?? ? ???? 1 ?? 0 00 ?? 000 ? 0 1"), // csrrw
		  BitPat("b??????? ????? ????? 101 ????? 11100 11") -> BitPat("b001 0 0 ?? ? ???? 1 ?? 0 00 ?? 000 ? 0 1"), // csrrwi
		  BitPat("b0000000 00001 00000 000 00000 11100 11") -> BitPat("b110 0 0 ?? ? ???? 1 ?? 0 00 ?? 000 ? 1 0"), // ebreak
		  BitPat("b0000000 00000 00000 000 00000 11100 11") -> BitPat("b110 0 0 ?? ? ???? 1 ?? 0 00 ?? 000 ? 0 1"), // ecall
		  BitPat("b0011000 00010 00000 000 00000 11100 11") -> BitPat("b001 0 0 ?? ? ???? 1 ?? 0 00 ?? 000 ? 0 1"), // mret
	),
    BitPat("b110 0 0 ?? ? ???? 1 ?? 0 00 ?? 000 ? 0 1") // inv
  )
  val input  = IO(Input(UInt(32.W)))
  val output  = IO(new DecodeControlOut)
  val decodeOut = decoder(input, table)
  output.badtrap := decodeOut(0, 0)
  output.goodtrap := decodeOut(1, 1)
  output.pcsrc := decodeOut(2, 2)
  output.pcaddrsrc := decodeOut(5, 3)
  output.memlen := decodeOut(7, 6)
  output.memmode := decodeOut(9, 8)
  output.regwsext := decodeOut(10, 10)
  output.regwritemux := decodeOut(12, 11)
  output.regwrite := decodeOut(13, 13)
  output.alumode := decodeOut(17, 14)
  output.alumux2 := decodeOut(18, 18)
  output.alumux1 := decodeOut(20, 19)
  output.srccast2 := decodeOut(21, 21)
  output.srccast1 := decodeOut(22, 22)
  output.insttype := decodeOut(25, 23)
}