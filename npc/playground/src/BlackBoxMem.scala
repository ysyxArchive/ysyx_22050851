import chisel3._
import chisel3.util.HasBlackBoxInline

class MemIO extends Bundle {
  val enable = Input(Bool())
  val isRead = Input(Bool())
  val addr   = Input(UInt(64.W))
  val len    = Input(UInt(4.W))
  val rdata  = Output(UInt(64.W))
  val wdata  = Input(UInt(64.W))
}

class BlackBoxMem extends BlackBox with HasBlackBoxInline {
  val io = IO(new MemIO);
  setInline(
    "BlackBoxMem.v",
    """import "DPI-C" function void mem_read(input [63:0] addr, input [3:0] len, output [63:0] data);
      |import "DPI-C" function void mem_write(input [63:0] addr, input [3:0] len, input [63:0] data);
      |module BlackBoxMem (
      |  input enable,
      |  input isRead,
      |  input [63:0] addr,
      |  input [3:0] len,
      |  input [63:0] wdata,
      |  output [63:0] rdata
      |);
      |  always @(posedge enable) begin
      |     if(enable && !isRead) mem_write(addr, len, wdata);
      |     if(enable && isRead) mem_read(addr, len, rdata);
      |  end
      |endmodule""".stripMargin
  )
}
