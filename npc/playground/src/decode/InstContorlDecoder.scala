
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
	val or = Value(9.U)
	val rem = Value(10.U)
	val remu = Value(11.U)
	val remuw = Value(12.U)
	val remw = Value(13.U)
	val ll = Value(14.U)
	val ra = Value(15.U)
	val rl = Value(16.U)
	val rlw = Value(17.U)
	val xor = Value(18.U)
}
object RegWriteMux extends ChiselEnum {
	val alu = Value(0.U)
	val snpc = Value(1.U)
	val mem = Value(2.U)
	val aluneg = Value(3.U)
	val alunotcarryandnotzero = Value(4.U)
	val csr = Value(5.U)
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
object PcCsr extends ChiselEnum {
	val origin = Value(0.U)
	val csr = Value(1.U)
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
	val regwrite = Output(Bool())
	val regwritemux = Output(UInt(3.W))
	val regwsext = Output(Bool())
	val memmode = Output(UInt(2.W))
	val memlen = Output(UInt(2.W))
	val pcaddrsrc = Output(UInt(3.W))
	val pcsrc = Output(Bool())
	val goodtrap = Output(Bool())
	val badtrap = Output(Bool())
	val csrsetmode = Output(UInt(2.W))
	val csrsource = Output(Bool())
	val pccsr = Output(Bool())
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
	 defaultout.regwrite := false.B
	 defaultout.regwritemux := 0.U
	 defaultout.regwsext := false.B
	 defaultout.memmode := MemMode.no.asUInt
	 defaultout.memlen := 0.U
	 defaultout.pcaddrsrc := PCAddrSrc.zero.asUInt
	 defaultout.pcsrc := false.B
	 defaultout.goodtrap := false.B
	 defaultout.badtrap := false.B
	 defaultout.csrsetmode := CsrSetMode.origin.asUInt
	 defaultout.csrsource := false.B
	 defaultout.pccsr := false.B
	 defaultout.csrbehave := CsrBehave.no.asUInt
defaultout
}
}

class InstContorlDecoder extends Module {
  val table = TruthTable(
    Map(
		  BitPat("b0000000 ????? ????? 000 ????? 01100 11") -> BitPat("b000 0 0 00 0 00000 1 000 0 00 ?? 000 ? 0 0 00 ? 0 00"), // add
		  BitPat("b??????? ????? ????? 000 ????? 00100 11") -> BitPat("b001 0 0 00 1 00000 1 000 0 00 ?? 000 ? 0 0 00 ? 0 00"), // addi
		  BitPat("b??????? ????? ????? 000 ????? 00110 11") -> BitPat("b001 0 0 00 1 00000 1 000 1 00 ?? 000 ? 0 0 00 ? 0 00"), // addiw
		  BitPat("b0000000 ????? ????? 000 ????? 01110 11") -> BitPat("b000 1 1 00 0 00000 1 000 1 00 ?? 000 ? 0 0 00 ? 0 00"), // addw
		  BitPat("b0000000 ????? ????? 111 ????? 01100 11") -> BitPat("b000 0 0 00 0 00001 1 000 0 00 ?? 000 ? 0 0 00 ? 0 00"), // and
		  BitPat("b??????? ????? ????? 111 ????? 00100 11") -> BitPat("b001 0 0 00 1 00001 1 000 0 00 ?? 000 ? 0 0 00 ? 0 00"), // andi
		  BitPat("b??????? ????? ????? ??? ????? 00101 11") -> BitPat("b010 0 0 01 1 00000 1 000 0 00 ?? 000 ? 0 0 00 ? 0 00"), // auipc
		  BitPat("b??????? ????? ????? 000 ????? 11000 11") -> BitPat("b011 0 0 00 0 00010 0 ??? 0 00 ?? 001 0 0 0 00 ? 0 00"), // beq
		  BitPat("b??????? ????? ????? 101 ????? 11000 11") -> BitPat("b011 0 0 00 0 00010 0 ??? 0 00 ?? 010 0 0 0 00 ? 0 00"), // bge
		  BitPat("b??????? ????? ????? 111 ????? 11000 11") -> BitPat("b011 0 0 00 0 00010 0 ??? 0 00 ?? 011 0 0 0 00 ? 0 00"), // bgeu
		  BitPat("b??????? ????? ????? 100 ????? 11000 11") -> BitPat("b011 0 0 00 0 00010 0 ??? 0 00 ?? 100 0 0 0 00 ? 0 00"), // blt
		  BitPat("b??????? ????? ????? 110 ????? 11000 11") -> BitPat("b011 0 0 00 0 00010 0 ??? 0 00 ?? 101 0 0 0 00 ? 0 00"), // bltu
		  BitPat("b??????? ????? ????? 001 ????? 11000 11") -> BitPat("b011 0 0 00 0 00010 0 ??? 0 00 ?? 110 0 0 0 00 ? 0 00"), // bne
		  BitPat("b0000001 ????? ????? 100 ????? 01100 11") -> BitPat("b000 0 0 00 0 00011 1 000 0 00 ?? 000 ? 0 0 00 ? 0 00"), // div
		  BitPat("b0000001 ????? ????? 101 ????? 01100 11") -> BitPat("b000 0 0 00 0 00100 1 000 0 00 ?? 000 ? 0 0 00 ? 0 00"), // divu
		  BitPat("b0000001 ????? ????? 101 ????? 01110 11") -> BitPat("b000 1 1 00 0 00101 1 000 1 00 ?? 000 ? 0 0 00 ? 0 00"), // divuw
		  BitPat("b0000001 ????? ????? 100 ????? 01110 11") -> BitPat("b000 1 1 00 0 00110 1 000 1 00 ?? 000 ? 0 0 00 ? 0 00"), // divw
		  BitPat("b??????? ????? ????? ??? ????? 11011 11") -> BitPat("b100 0 0 ?? ? 00000 1 001 0 00 ?? 111 0 0 0 00 ? 0 00"), // jal
		  BitPat("b??????? ????? ????? 000 ????? 11001 11") -> BitPat("b001 0 0 ?? ? 00000 1 001 0 00 ?? 111 1 0 0 00 ? 0 00"), // jalr
		  BitPat("b??????? ????? ????? 000 ????? 00000 11") -> BitPat("b001 0 0 00 1 00000 1 010 0 01 00 000 ? 0 0 00 ? 0 00"), // lb
		  BitPat("b??????? ????? ????? 100 ????? 00000 11") -> BitPat("b001 0 0 00 1 00000 1 010 0 10 00 000 ? 0 0 00 ? 0 00"), // lbu
		  BitPat("b??????? ????? ????? 011 ????? 00000 11") -> BitPat("b001 0 0 00 1 00000 1 010 0 01 01 000 ? 0 0 00 ? 0 00"), // ld
		  BitPat("b??????? ????? ????? 001 ????? 00000 11") -> BitPat("b001 0 0 00 1 00000 1 010 0 01 10 000 ? 0 0 00 ? 0 00"), // lh
		  BitPat("b??????? ????? ????? 101 ????? 00000 11") -> BitPat("b001 0 0 00 1 00000 1 010 0 10 10 000 ? 0 0 00 ? 0 00"), // lhu
		  BitPat("b??????? ????? ????? ??? ????? 01101 11") -> BitPat("b010 0 0 10 1 00000 1 000 0 00 ?? 000 ? 0 0 00 ? 0 00"), // lui
		  BitPat("b??????? ????? ????? 010 ????? 00000 11") -> BitPat("b001 0 0 00 1 00000 1 010 0 01 11 000 ? 0 0 00 ? 0 00"), // lw
		  BitPat("b??????? ????? ????? 110 ????? 00000 11") -> BitPat("b001 0 0 00 1 00000 1 010 0 10 11 000 ? 0 0 00 ? 0 00"), // lwu
		  BitPat("b0000001 ????? ????? 000 ????? 01100 11") -> BitPat("b000 0 0 00 0 00111 1 000 0 00 ?? 000 ? 0 0 00 ? 0 00"), // mul
		  BitPat("b0000001 ????? ????? 000 ????? 01110 11") -> BitPat("b000 1 1 00 0 01000 1 000 1 00 ?? 000 ? 0 0 00 ? 0 00"), // mulw
		  BitPat("b0000000 ????? ????? 110 ????? 01100 11") -> BitPat("b000 0 0 00 0 01001 1 000 0 00 ?? 000 ? 0 0 00 ? 0 00"), // or
		  BitPat("b??????? ????? ????? 110 ????? 00100 11") -> BitPat("b001 0 0 00 1 01001 1 000 0 00 ?? 000 ? 0 0 00 ? 0 00"), // ori
		  BitPat("b0000001 ????? ????? 110 ????? 01100 11") -> BitPat("b000 0 0 00 0 01010 1 000 0 00 ?? 000 ? 0 0 00 ? 0 00"), // rem
		  BitPat("b0000001 ????? ????? 111 ????? 01100 11") -> BitPat("b000 0 0 00 0 01011 1 000 0 00 ?? 000 ? 0 0 00 ? 0 00"), // remu
		  BitPat("b0000001 ????? ????? 111 ????? 01110 11") -> BitPat("b000 1 1 00 0 01100 1 000 1 00 ?? 000 ? 0 0 00 ? 0 00"), // remuw
		  BitPat("b0000001 ????? ????? 110 ????? 01110 11") -> BitPat("b000 1 1 00 0 01101 1 000 1 00 ?? 000 ? 0 0 00 ? 0 00"), // remw
		  BitPat("b??????? ????? ????? 000 ????? 01000 11") -> BitPat("b101 0 0 00 1 00000 0 ??? 0 11 00 000 ? 0 0 00 ? 0 00"), // sb
		  BitPat("b??????? ????? ????? 011 ????? 01000 11") -> BitPat("b101 0 0 00 1 00000 0 ??? 0 11 01 000 ? 0 0 00 ? 0 00"), // sd
		  BitPat("b??????? ????? ????? 001 ????? 01000 11") -> BitPat("b101 0 0 00 1 00000 0 ??? 0 11 10 000 ? 0 0 00 ? 0 00"), // sh
		  BitPat("b0000000 ????? ????? 001 ????? 01100 11") -> BitPat("b000 0 0 00 0 01110 1 000 0 00 ?? 000 ? 0 0 00 ? 0 00"), // sll
		  BitPat("b000000? ????? ????? 001 ????? 00100 11") -> BitPat("b001 0 0 00 1 01110 1 000 0 00 ?? 000 ? 0 0 00 ? 0 00"), // slli
		  BitPat("b0000000 ????? ????? 001 ????? 00110 11") -> BitPat("b001 1 0 00 1 01110 1 000 1 00 ?? 000 ? 0 0 00 ? 0 00"), // slliw
		  BitPat("b0000000 ????? ????? 001 ????? 01110 11") -> BitPat("b000 1 0 00 0 01110 1 000 1 00 ?? 000 ? 0 0 00 ? 0 00"), // sllw
		  BitPat("b0000000 ????? ????? 010 ????? 01100 11") -> BitPat("b000 0 0 00 0 00010 1 011 1 00 ?? 000 ? 0 0 00 ? 0 00"), // slt
		  BitPat("b??????? ????? ????? 010 ????? 00100 11") -> BitPat("b001 0 0 00 1 00010 1 011 1 00 ?? 000 ? 0 0 00 ? 0 00"), // slti
		  BitPat("b??????? ????? ????? 011 ????? 00100 11") -> BitPat("b001 0 0 00 1 00010 1 100 1 00 ?? 000 ? 0 0 00 ? 0 00"), // sltiu
		  BitPat("b0000000 ????? ????? 011 ????? 01100 11") -> BitPat("b000 0 0 00 0 00010 1 100 1 00 ?? 000 ? 0 0 00 ? 0 00"), // sltu
		  BitPat("b0100000 ????? ????? 001 ????? 01100 11") -> BitPat("b000 0 0 00 0 01111 1 000 0 00 ?? 000 ? 0 0 00 ? 0 00"), // sra
		  BitPat("b010000? ????? ????? 101 ????? 00100 11") -> BitPat("b001 0 0 00 1 01111 1 000 0 00 ?? 000 ? 0 0 00 ? 0 00"), // srai
		  BitPat("b0100000 ????? ????? 101 ????? 00110 11") -> BitPat("b001 1 0 00 1 01111 1 000 1 00 ?? 000 ? 0 0 00 ? 0 00"), // sraiw
		  BitPat("b0100000 ????? ????? 101 ????? 01110 11") -> BitPat("b000 1 0 00 0 01111 1 000 1 00 ?? 000 ? 0 0 00 ? 0 00"), // sraw
		  BitPat("b0000000 ????? ????? 101 ????? 01100 11") -> BitPat("b000 0 0 00 0 10000 1 000 0 00 ?? 000 ? 0 0 00 ? 0 00"), // srl
		  BitPat("b000000? ????? ????? 101 ????? 00100 11") -> BitPat("b001 0 0 00 1 10000 1 000 0 00 ?? 000 ? 0 0 00 ? 0 00"), // srli
		  BitPat("b0000000 ????? ????? 101 ????? 00110 11") -> BitPat("b001 1 0 00 1 10001 1 000 1 00 ?? 000 ? 0 0 00 ? 0 00"), // srliw
		  BitPat("b0000000 ????? ????? 101 ????? 01110 11") -> BitPat("b000 1 0 00 0 10001 1 000 1 00 ?? 000 ? 0 0 00 ? 0 00"), // srlw
		  BitPat("b0100000 ????? ????? 000 ????? 01100 11") -> BitPat("b000 0 0 00 0 00010 1 000 0 00 ?? 000 ? 0 0 00 ? 0 00"), // sub
		  BitPat("b0100000 ????? ????? 000 ????? 01110 11") -> BitPat("b000 1 1 00 0 00010 1 000 1 00 ?? 000 ? 0 0 00 ? 0 00"), // subw
		  BitPat("b??????? ????? ????? 010 ????? 01000 11") -> BitPat("b101 0 0 00 1 00000 0 ??? 0 11 11 000 ? 0 0 00 ? 0 00"), // sw
		  BitPat("b0000000 ????? ????? 100 ????? 01100 11") -> BitPat("b000 0 0 00 0 10010 1 000 0 00 ?? 000 ? 0 0 00 ? 0 00"), // xor
		  BitPat("b??????? ????? ????? 100 ????? 00100 11") -> BitPat("b001 0 0 00 1 10010 1 000 0 00 ?? 000 ? 0 0 00 ? 0 00"), // xori
		  BitPat("b??????? ????? ????? 011 ????? 11100 11") -> BitPat("b001 0 0 ?? ? 00000 1 101 0 00 ?? 000 ? 0 0 01 0 0 00"), // csrrc
		  BitPat("b??????? ????? ????? 111 ????? 11100 11") -> BitPat("b001 0 0 ?? ? 00000 1 101 0 00 ?? 000 ? 0 0 01 1 0 00"), // csrrci
		  BitPat("b??????? ????? ????? 010 ????? 11100 11") -> BitPat("b001 0 0 ?? ? 00000 1 101 0 00 ?? 000 ? 0 0 10 0 0 00"), // csrrs
		  BitPat("b??????? ????? ????? 110 ????? 11100 11") -> BitPat("b001 0 0 ?? ? 00000 1 101 0 00 ?? 000 ? 0 0 10 1 0 00"), // csrrsi
		  BitPat("b??????? ????? ????? 001 ????? 11100 11") -> BitPat("b001 0 0 ?? ? 00000 1 101 0 00 ?? 000 ? 0 0 11 0 0 00"), // csrrw
		  BitPat("b??????? ????? ????? 101 ????? 11100 11") -> BitPat("b001 0 0 ?? ? 00000 1 101 0 00 ?? 000 ? 0 0 11 1 0 00"), // csrrwi
		  BitPat("b0000000 00001 00000 000 00000 11100 11") -> BitPat("b110 0 0 ?? ? 00000 0 ??? 0 00 ?? 000 ? 1 0 00 ? 0 00"), // ebreak
		  BitPat("b0000000 00000 00000 000 00000 11100 11") -> BitPat("b110 0 0 ?? ? 00000 0 ??? 0 00 ?? 111 ? 0 0 00 ? 1 01"), // ecall
		  BitPat("b0011000 00010 00000 000 00000 11100 11") -> BitPat("b001 0 0 ?? ? 00000 0 ??? 0 00 ?? 111 ? 0 0 00 ? 1 10"), // mret
	),
    BitPat("b110 0 0 ?? ? 00000 0 ??? 0 00 ?? 000 ? 0 1 00 ? 0 00") // inv
  )
  val input  = IO(Input(UInt(32.W)))
  val output  = IO(new ExeControlIn)
  val decodeOut = decoder(input, table)
  output.csrbehave := decodeOut(1, 0)
  output.pccsr := decodeOut(2, 2)
  output.csrsource := decodeOut(3, 3)
  output.csrsetmode := decodeOut(5, 4)
  output.badtrap := decodeOut(6, 6)
  output.goodtrap := decodeOut(7, 7)
  output.pcsrc := decodeOut(8, 8)
  output.pcaddrsrc := decodeOut(11, 9)
  output.memlen := decodeOut(13, 12)
  output.memmode := decodeOut(15, 14)
  output.regwsext := decodeOut(16, 16)
  output.regwritemux := decodeOut(19, 17)
  output.regwrite := decodeOut(20, 20)
  output.alumode := decodeOut(25, 21)
  output.alumux2 := decodeOut(26, 26)
  output.alumux1 := decodeOut(28, 27)
  output.srccast2 := decodeOut(29, 29)
  output.srccast1 := decodeOut(30, 30)
  output.insttype := decodeOut(33, 31)
}