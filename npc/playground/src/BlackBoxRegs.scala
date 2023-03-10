import chisel3._
import chisel3.util.HasBlackBoxInline
class BlackBoxRegs extends BlackBox with HasBlackBoxInline {
  val io = IO(new Bundle {
    val regs  = Input(Vec(32, UInt(64.W)))
    val pc    = Input(UInt(64.W))
    val waddr = Input(UInt(5.W))
    val wdata = Input(UInt(64.W))
  })
  setInline(
    "BlackBoxRegs.v",
    """import "DPI-C" function void set_gpr_ptr(input logic [63:0] a []);
      |module BlackBoxRegs (
      |  input [63:0] regs_0,
      |  input [63:0] regs_1,
      |  input [63:0] regs_2,
      |  input [63:0] regs_3,
      |  input [63:0] regs_4,
      |  input [63:0] regs_5,
      |  input [63:0] regs_6,
      |  input [63:0] regs_7,
      |  input [63:0] regs_8,
      |  input [63:0] regs_9,
      |  input [63:0] regs_10,
      |  input [63:0] regs_11,
      |  input [63:0] regs_12,
      |  input [63:0] regs_13,
      |  input [63:0] regs_14,
      |  input [63:0] regs_15,
      |  input [63:0] regs_16,
      |  input [63:0] regs_17,
      |  input [63:0] regs_18,
      |  input [63:0] regs_19,
      |  input [63:0] regs_20,
      |  input [63:0] regs_21,
      |  input [63:0] regs_22,
      |  input [63:0] regs_23,
      |  input [63:0] regs_24,
      |  input [63:0] regs_25,
      |  input [63:0] regs_26,
      |  input [63:0] regs_27,
      |  input [63:0] regs_28,
      |  input [63:0] regs_29,
      |  input [63:0] regs_30,
      |  input [63:0] regs_31,
      |  input [63:0] pc, 
      |  input [4:0] waddr,
      |  input [63:0] wdata
      |);
      |  wire [63:0] regs [0:32];
      |  assign regs[0] = 64'h0;
      |  assign regs[1] = waddr == 5'h1 ? wdata : regs_1;
      |  assign regs[2] = waddr == 5'd2 ? wdata : regs_2;
      |  assign regs[3] = waddr == 5'd3 ? wdata : regs_3;
      |  assign regs[4] = waddr == 5'd4 ? wdata : regs_4;
      |  assign regs[5] = waddr == 5'd5 ? wdata : regs_5;
      |  assign regs[6] = waddr == 5'd6 ? wdata : regs_6;
      |  assign regs[7] = waddr == 5'd7 ? wdata : regs_7;
      |  assign regs[8] = waddr == 5'd8 ? wdata : regs_8;
      |  assign regs[9] = waddr == 5'd9 ? wdata : regs_9;
      |  assign regs[10] = waddr == 5'd10 ? wdata : regs_10;
      |  assign regs[11] = waddr == 5'd11 ? wdata : regs_11;
      |  assign regs[12] = waddr == 5'd12 ? wdata : regs_12;
      |  assign regs[13] = waddr == 5'd13 ? wdata : regs_13;
      |  assign regs[14] = waddr == 5'd14 ? wdata : regs_14;
      |  assign regs[15] = waddr == 5'd15 ? wdata : regs_15;
      |  assign regs[16] = waddr == 5'd16 ? wdata : regs_16;
      |  assign regs[17] = waddr == 5'd17 ? wdata : regs_17;
      |  assign regs[18] = waddr == 5'd18 ? wdata : regs_18;
      |  assign regs[19] = waddr == 5'd19 ? wdata : regs_19;
      |  assign regs[20] = waddr == 5'd20 ? wdata : regs_20;
      |  assign regs[21] = waddr == 5'd21 ? wdata : regs_21;
      |  assign regs[22] = waddr == 5'd22 ? wdata : regs_22;
      |  assign regs[23] = waddr == 5'd23 ? wdata : regs_23;
      |  assign regs[24] = waddr == 5'd24 ? wdata : regs_24;
      |  assign regs[25] = waddr == 5'd25 ? wdata : regs_25;
      |  assign regs[26] = waddr == 5'd26 ? wdata : regs_26;
      |  assign regs[27] = waddr == 5'd27 ? wdata : regs_27;
      |  assign regs[28] = waddr == 5'd28 ? wdata : regs_28;
      |  assign regs[29] = waddr == 5'd29 ? wdata : regs_29;
      |  assign regs[30] = waddr == 5'd30 ? wdata : regs_30;
      |  assign regs[31] = waddr == 5'd31 ? wdata : regs_31;
      |  assign regs[32] = pc;
      |  initial set_gpr_ptr(regs);  // regs为通用寄存器的二维数组变量
      |endmodule""".stripMargin
  )
}
