import chisel3._

class RegisterFile(val addr_width: Int, val data_width: Int) extends Module {
  val io = IO(new Bundle {
    val wdata = Input(UInt(data_width.W))
    val waddr = Input(UInt(addr_width.W))
    val wen   = Input(Bool())
    val rdata = Input(UInt(data_width.W))
    val raddr = Input(UInt(addr_width.W))
    val ren   = Input(Bool())
  })

  val regs = Vec(addr_width, Reg(UInt(data_width.W)))

  when(io.wen) {
    regs(io.waddr) := io.wdata
  }
  when(io.ren) {
    io.rdata := regs(io.raddr)
  }
}
// module RegisterFile #(addr_width = 1, data_width = 1) (
//   input clk,
//   input [data_width-1:0] wdata,
//   input [addr_width-1:0] waddr,
//   input wen
// );
//   reg [data_width-1:0] rf [addr_width-1:0];
//   always @(posedge clk) begin
//     if (wen) rf[waddr] <= wdata;
//   end
// endmodule
