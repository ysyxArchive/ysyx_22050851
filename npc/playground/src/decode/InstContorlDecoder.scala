
// AUTO GENERATED CODE DO NOT EDIT
package decode

import chisel3._
import chisel3.util.BitPat
import chisel3.util.experimental.decode._
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
	val divuw = Value(5.U)
	val divw = Value(6.U)
	val mul = Value(7.U)
	val mulw = Value(8.U)
	val mulh = Value(9.U)
	val mulhsu = Value(10.U)
	val mulhu = Value(11.U)
	val or = Value(12.U)
	val rem = Value(13.U)
	val remu = Value(14.U)
	val remuw = Value(15.U)
	val remw = Value(16.U)
	val ll = Value(17.U)
	val ra = Value(18.U)
	val rl = Value(19.U)
	val rlw = Value(20.U)
	val xor = Value(21.U)
}
object RegWriteMux extends ChiselEnum {
	val alu = Value(0.U)
	val no = Value(1.U)
	val snpc = Value(2.U)
	val mem = Value(3.U)
	val aluneg = Value(4.U)
	val alunotcarryandnotzero = Value(5.U)
	val csr = Value(6.U)
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
	val alucarryorzero = Value(3.U)
	val aluneg = Value(4.U)
	val alunotcarryandnotzero = Value(5.U)
	val alunotzero = Value(6.U)
	val one = Value(7.U)
}
object PcSrc extends ChiselEnum {
	val pc = Value(0.U)
	val src1 = Value(1.U)
	val csr = Value(2.U)
}
object CsrSetMode extends ChiselEnum {
	val origin = Value(0.U)
	val clear = Value(1.U)
	val set = Value(2.U)
	val write = Value(3.U)
}
object CsrSource extends ChiselEnum {
	val src1 = Value(0.U)
	val uimm = Value(1.U)
}
object CsrBehave extends ChiselEnum {
	val no = Value(0.U)
	val ecall = Value(1.U)
	val mret = Value(2.U)
}
class ExeControlIn extends Bundle {
	val insttype = Output(UInt(3.W))
	val srccast1 = Output(Bool())
	val srccast2 = Output(Bool())
	val alumux1 = Output(UInt(2.W))
	val alumux2 = Output(Bool())
	val alumode = Output(UInt(5.W))
	val regwritemux = Output(UInt(3.W))
	val regwsext = Output(Bool())
	val memmode = Output(UInt(2.W))
	val memlen = Output(UInt(2.W))
	val pcaddrsrc = Output(UInt(3.W))
	val pcsrc = Output(UInt(2.W))
	val goodtrap = Output(Bool())
	val badtrap = Output(Bool())
	val csrsetmode = Output(UInt(2.W))
	val csrsource = Output(Bool())
	val csrbehave = Output(UInt(2.W))
}

object ExeControlIn{
  def default() = {
    val defaultout = Wire(new ExeControlIn);
	 defaultout.insttype := 0.U
	 defaultout.srccast1 := false.B
	 defaultout.srccast2 := false.B
	 defaultout.alumux1 := 0.U
	 defaultout.alumux2 := false.B
	 defaultout.alumode := AluMode.add.asUInt
	 defaultout.regwritemux := RegWriteMux.no.asUInt
	 defaultout.regwsext := false.B
	 defaultout.memmode := MemMode.no.asUInt
	 defaultout.memlen := 0.U
	 defaultout.pcaddrsrc := PCAddrSrc.zero.asUInt
	 defaultout.pcsrc := 0.U
	 defaultout.goodtrap := false.B
	 defaultout.badtrap := false.B
	 defaultout.csrsetmode := CsrSetMode.origin.asUInt
	 defaultout.csrsource := false.B
	 defaultout.csrbehave := CsrBehave.no.asUInt
defaultout
}
}

class InstContorlDecoder extends Module {
  val table = TruthTable(
    Map(
		  BitPat("b0000000 ????? ????? 000 ????? 01100 11") -> BitPat("b000 0 0 00 0 00000 000 0 00 ?? 000 ?? 0 0 00 ? 00"), // add
		  BitPat("b??????? ????? ????? 000 ????? 00100 11") -> BitPat("b001 0 ? 00 1 00000 000 0 00 ?? 000 ?? 0 0 00 ? 00"), // addi
		  BitPat("b??????? ????? ????? 000 ????? 00110 11") -> BitPat("b001 0 ? 00 1 00000 000 1 00 ?? 000 ?? 0 0 00 ? 00"), // addiw
		  BitPat("b0000000 ????? ????? 000 ????? 01110 11") -> BitPat("b000 1 1 00 0 00000 000 1 00 ?? 000 ?? 0 0 00 ? 00"), // addw
		  BitPat("b0000000 ????? ????? 111 ????? 01100 11") -> BitPat("b000 0 0 00 0 00001 000 0 00 ?? 000 ?? 0 0 00 ? 00"), // and
		  BitPat("b??????? ????? ????? 111 ????? 00100 11") -> BitPat("b001 0 ? 00 1 00001 000 0 00 ?? 000 ?? 0 0 00 ? 00"), // andi
		  BitPat("b??????? ????? ????? ??? ????? 00101 11") -> BitPat("b010 ? ? 01 1 00000 000 0 00 ?? 000 ?? 0 0 00 ? 00"), // auipc
		  BitPat("b??????? ????? ????? 000 ????? 11000 11") -> BitPat("b011 0 0 00 0 00010 001 ? 00 ?? 001 00 0 0 00 ? 00"), // beq
		  BitPat("b??????? ????? ????? 101 ????? 11000 11") -> BitPat("b011 0 0 00 0 00010 001 ? 00 ?? 010 00 0 0 00 ? 00"), // bge
		  BitPat("b??????? ????? ????? 111 ????? 11000 11") -> BitPat("b011 0 0 00 0 00010 001 ? 00 ?? 011 00 0 0 00 ? 00"), // bgeu
		  BitPat("b??????? ????? ????? 100 ????? 11000 11") -> BitPat("b011 0 0 00 0 00010 001 ? 00 ?? 100 00 0 0 00 ? 00"), // blt
		  BitPat("b??????? ????? ????? 110 ????? 11000 11") -> BitPat("b011 0 0 00 0 00010 001 ? 00 ?? 101 00 0 0 00 ? 00"), // bltu
		  BitPat("b??????? ????? ????? 001 ????? 11000 11") -> BitPat("b011 0 0 00 0 00010 001 ? 00 ?? 110 00 0 0 00 ? 00"), // bne
		  BitPat("b0000001 ????? ????? 100 ????? 01100 11") -> BitPat("b000 0 0 00 0 00011 000 0 00 ?? 000 ?? 0 0 00 ? 00"), // div
		  BitPat("b0000001 ????? ????? 101 ????? 01100 11") -> BitPat("b000 0 0 00 0 00100 000 0 00 ?? 000 ?? 0 0 00 ? 00"), // divu
		  BitPat("b0000001 ????? ????? 101 ????? 01110 11") -> BitPat("b000 1 1 00 0 00101 000 1 00 ?? 000 ?? 0 0 00 ? 00"), // divuw
		  BitPat("b0000001 ????? ????? 100 ????? 01110 11") -> BitPat("b000 1 1 00 0 00110 000 1 00 ?? 000 ?? 0 0 00 ? 00"), // divw
		  BitPat("b??????? ????? ????? ??? ????? 11011 11") -> BitPat("b100 ? ? ?? ? 00000 010 0 00 ?? 111 00 0 0 00 ? 00"), // jal
		  BitPat("b??????? ????? ????? 000 ????? 11001 11") -> BitPat("b001 0 ? ?? ? 00000 010 0 00 ?? 111 01 0 0 00 ? 00"), // jalr
		  BitPat("b??????? ????? ????? 000 ????? 00000 11") -> BitPat("b001 0 ? 00 1 00000 011 0 01 00 000 ?? 0 0 00 ? 00"), // lb
		  BitPat("b??????? ????? ????? 100 ????? 00000 11") -> BitPat("b001 0 ? 00 1 00000 011 0 10 00 000 ?? 0 0 00 ? 00"), // lbu
		  BitPat("b??????? ????? ????? 011 ????? 00000 11") -> BitPat("b001 0 ? 00 1 00000 011 0 01 01 000 ?? 0 0 00 ? 00"), // ld
		  BitPat("b??????? ????? ????? 001 ????? 00000 11") -> BitPat("b001 0 ? 00 1 00000 011 0 01 10 000 ?? 0 0 00 ? 00"), // lh
		  BitPat("b??????? ????? ????? 101 ????? 00000 11") -> BitPat("b001 0 ? 00 1 00000 011 0 10 10 000 ?? 0 0 00 ? 00"), // lhu
		  BitPat("b??????? ????? ????? ??? ????? 01101 11") -> BitPat("b010 ? ? 10 1 00000 000 0 00 ?? 000 ?? 0 0 00 ? 00"), // lui
		  BitPat("b??????? ????? ????? 010 ????? 00000 11") -> BitPat("b001 0 ? 00 1 00000 011 0 01 11 000 ?? 0 0 00 ? 00"), // lw
		  BitPat("b??????? ????? ????? 110 ????? 00000 11") -> BitPat("b001 0 ? 00 1 00000 011 0 10 11 000 ?? 0 0 00 ? 00"), // lwu
		  BitPat("b0000001 ????? ????? 000 ????? 01100 11") -> BitPat("b000 0 0 00 0 00111 000 0 00 ?? 000 ?? 0 0 00 ? 00"), // mul
		  BitPat("b0000001 ????? ????? 000 ????? 01110 11") -> BitPat("b000 1 1 00 0 01000 000 1 00 ?? 000 ?? 0 0 00 ? 00"), // mulw
		  BitPat("b0000001 ????? ????? 001 ????? 01100 11") -> BitPat("b000 0 0 00 0 01001 000 0 00 ?? 000 ?? 0 0 00 ? 00"), // mulh
		  BitPat("b0000001 ????? ????? 010 ????? 01100 11") -> BitPat("b000 0 0 00 0 01010 000 0 00 ?? 000 ?? 0 0 00 ? 00"), // mulhsu
		  BitPat("b0000001 ????? ????? 011 ????? 01100 11") -> BitPat("b000 0 0 00 0 01011 000 0 00 ?? 000 ?? 0 0 00 ? 00"), // mulhu
		  BitPat("b0000000 ????? ????? 110 ????? 01100 11") -> BitPat("b000 0 0 00 0 01100 000 0 00 ?? 000 ?? 0 0 00 ? 00"), // or
		  BitPat("b??????? ????? ????? 110 ????? 00100 11") -> BitPat("b001 0 ? 00 1 01100 000 0 00 ?? 000 ?? 0 0 00 ? 00"), // ori
		  BitPat("b0000001 ????? ????? 110 ????? 01100 11") -> BitPat("b000 0 0 00 0 01101 000 0 00 ?? 000 ?? 0 0 00 ? 00"), // rem
		  BitPat("b0000001 ????? ????? 111 ????? 01100 11") -> BitPat("b000 0 0 00 0 01110 000 0 00 ?? 000 ?? 0 0 00 ? 00"), // remu
		  BitPat("b0000001 ????? ????? 111 ????? 01110 11") -> BitPat("b000 1 1 00 0 01111 000 1 00 ?? 000 ?? 0 0 00 ? 00"), // remuw
		  BitPat("b0000001 ????? ????? 110 ????? 01110 11") -> BitPat("b000 1 1 00 0 10000 000 1 00 ?? 000 ?? 0 0 00 ? 00"), // remw
		  BitPat("b??????? ????? ????? 000 ????? 01000 11") -> BitPat("b101 0 0 00 1 00000 001 ? 11 00 000 ?? 0 0 00 ? 00"), // sb
		  BitPat("b??????? ????? ????? 011 ????? 01000 11") -> BitPat("b101 0 0 00 1 00000 001 ? 11 01 000 ?? 0 0 00 ? 00"), // sd
		  BitPat("b??????? ????? ????? 001 ????? 01000 11") -> BitPat("b101 0 0 00 1 00000 001 ? 11 10 000 ?? 0 0 00 ? 00"), // sh
		  BitPat("b0000000 ????? ????? 001 ????? 01100 11") -> BitPat("b000 0 0 00 0 10001 000 0 00 ?? 000 ?? 0 0 00 ? 00"), // sll
		  BitPat("b000000? ????? ????? 001 ????? 00100 11") -> BitPat("b001 0 ? 00 1 10001 000 0 00 ?? 000 ?? 0 0 00 ? 00"), // slli
		  BitPat("b0000000 ????? ????? 001 ????? 00110 11") -> BitPat("b001 1 ? 00 1 10001 000 1 00 ?? 000 ?? 0 0 00 ? 00"), // slliw
		  BitPat("b0000000 ????? ????? 001 ????? 01110 11") -> BitPat("b000 1 0 00 0 10001 000 1 00 ?? 000 ?? 0 0 00 ? 00"), // sllw
		  BitPat("b0000000 ????? ????? 010 ????? 01100 11") -> BitPat("b000 0 0 00 0 00010 100 1 00 ?? 000 ?? 0 0 00 ? 00"), // slt
		  BitPat("b??????? ????? ????? 010 ????? 00100 11") -> BitPat("b001 0 ? 00 1 00010 100 1 00 ?? 000 ?? 0 0 00 ? 00"), // slti
		  BitPat("b??????? ????? ????? 011 ????? 00100 11") -> BitPat("b001 0 ? 00 1 00010 101 1 00 ?? 000 ?? 0 0 00 ? 00"), // sltiu
		  BitPat("b0000000 ????? ????? 011 ????? 01100 11") -> BitPat("b000 0 0 00 0 00010 101 1 00 ?? 000 ?? 0 0 00 ? 00"), // sltu
		  BitPat("b0100000 ????? ????? 001 ????? 01100 11") -> BitPat("b000 0 0 00 0 10010 000 0 00 ?? 000 ?? 0 0 00 ? 00"), // sra
		  BitPat("b010000? ????? ????? 101 ????? 00100 11") -> BitPat("b001 0 ? 00 1 10010 000 0 00 ?? 000 ?? 0 0 00 ? 00"), // srai
		  BitPat("b0100000 ????? ????? 101 ????? 00110 11") -> BitPat("b001 1 ? 00 1 10010 000 1 00 ?? 000 ?? 0 0 00 ? 00"), // sraiw
		  BitPat("b0100000 ????? ????? 101 ????? 01110 11") -> BitPat("b000 1 0 00 0 10010 000 1 00 ?? 000 ?? 0 0 00 ? 00"), // sraw
		  BitPat("b0000000 ????? ????? 101 ????? 01100 11") -> BitPat("b000 0 0 00 0 10011 000 0 00 ?? 000 ?? 0 0 00 ? 00"), // srl
		  BitPat("b000000? ????? ????? 101 ????? 00100 11") -> BitPat("b001 0 ? 00 1 10011 000 0 00 ?? 000 ?? 0 0 00 ? 00"), // srli
		  BitPat("b0000000 ????? ????? 101 ????? 00110 11") -> BitPat("b001 1 ? 00 1 10100 000 1 00 ?? 000 ?? 0 0 00 ? 00"), // srliw
		  BitPat("b0000000 ????? ????? 101 ????? 01110 11") -> BitPat("b000 1 0 00 0 10100 000 1 00 ?? 000 ?? 0 0 00 ? 00"), // srlw
		  BitPat("b0100000 ????? ????? 000 ????? 01100 11") -> BitPat("b000 0 0 00 0 00010 000 0 00 ?? 000 ?? 0 0 00 ? 00"), // sub
		  BitPat("b0100000 ????? ????? 000 ????? 01110 11") -> BitPat("b000 1 1 00 0 00010 000 1 00 ?? 000 ?? 0 0 00 ? 00"), // subw
		  BitPat("b??????? ????? ????? 010 ????? 01000 11") -> BitPat("b101 0 0 00 1 00000 001 ? 11 11 000 ?? 0 0 00 ? 00"), // sw
		  BitPat("b0000000 ????? ????? 100 ????? 01100 11") -> BitPat("b000 0 0 00 0 10101 000 0 00 ?? 000 ?? 0 0 00 ? 00"), // xor
		  BitPat("b??????? ????? ????? 100 ????? 00100 11") -> BitPat("b001 0 ? 00 1 10101 000 0 00 ?? 000 ?? 0 0 00 ? 00"), // xori
		  BitPat("b??????? ????? ????? 011 ????? 11100 11") -> BitPat("b001 0 ? ?? ? 00000 110 0 00 ?? 000 ?? 0 0 01 0 00"), // csrrc
		  BitPat("b??????? ????? ????? 111 ????? 11100 11") -> BitPat("b001 ? ? ?? ? 00000 110 0 00 ?? 000 ?? 0 0 01 1 00"), // csrrci
		  BitPat("b??????? ????? ????? 010 ????? 11100 11") -> BitPat("b001 0 ? ?? ? 00000 110 0 00 ?? 000 ?? 0 0 10 0 00"), // csrrs
		  BitPat("b??????? ????? ????? 110 ????? 11100 11") -> BitPat("b001 ? ? ?? ? 00000 110 0 00 ?? 000 ?? 0 0 10 1 00"), // csrrsi
		  BitPat("b??????? ????? ????? 001 ????? 11100 11") -> BitPat("b001 0 ? ?? ? 00000 110 0 00 ?? 000 ?? 0 0 11 0 00"), // csrrw
		  BitPat("b??????? ????? ????? 101 ????? 11100 11") -> BitPat("b001 ? ? ?? ? 00000 110 0 00 ?? 000 ?? 0 0 11 1 00"), // csrrwi
		  BitPat("b0000000 00001 00000 000 00000 11100 11") -> BitPat("b110 ? ? ?? ? 00000 001 ? 00 ?? 000 ?? 1 0 00 ? 00"), // ebreak
		  BitPat("b0000000 00000 00000 000 00000 11100 11") -> BitPat("b110 ? ? ?? ? 00000 001 ? 00 ?? 111 10 0 0 00 ? 01"), // ecall
		  BitPat("b0011000 00010 00000 000 00000 11100 11") -> BitPat("b001 ? ? ?? ? 00000 001 ? 00 ?? 111 10 0 0 00 ? 10"), // mret
	),
    BitPat("b110 ? ? ?? ? 00000 001 ? 00 ?? 000 ?? 0 1 00 ? 00") // inv
  )
  val input  = IO(Input(UInt(32.W)))
  val output  = IO(new ExeControlIn)
  val decodeOut = decoder(input, table)
  output.csrbehave := decodeOut(1, 0)
  output.csrsource := decodeOut(2, 2)
  output.csrsetmode := decodeOut(4, 3)
  output.badtrap := decodeOut(5, 5)
  output.goodtrap := decodeOut(6, 6)
  output.pcsrc := decodeOut(8, 7)
  output.pcaddrsrc := decodeOut(11, 9)
  output.memlen := decodeOut(13, 12)
  output.memmode := decodeOut(15, 14)
  output.regwsext := decodeOut(16, 16)
  output.regwritemux := decodeOut(19, 17)
  output.alumode := decodeOut(24, 20)
  output.alumux2 := decodeOut(25, 25)
  output.alumux1 := decodeOut(27, 26)
  output.srccast2 := decodeOut(28, 28)
  output.srccast1 := decodeOut(29, 29)
  output.insttype := decodeOut(32, 30)
}