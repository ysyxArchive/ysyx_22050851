import chisel3._
import chisel3.util.HasBlackBoxInline

class MemIO extends Bundle {
  val enable = Input(Bool())
  val isRead = Input(Bool())
  val addr   = Input(UInt(64.W))
<<<<<<< HEAD:npc/playground/src/BlackBoxMem.scala
  val len    = Input(UInt(4.W))
=======
  val mask   = Input(UInt(8.W))
>>>>>>> npc:npc/playground/src/mem/BlackBoxMem.scala
  val rdata  = Output(UInt(64.W))
  val wdata  = Input(UInt(64.W))
  val clock  = Input(Clock())
}

class BlackBoxMem extends BlackBox with HasBlackBoxInline {
  val io = IO(new MemIO);
  setInline(
    "BlackBoxMem.v",
<<<<<<< HEAD:npc/playground/src/BlackBoxMem.scala
    """import "DPI-C" function void mem_read(input [63:0] addr, input [3:0] len, output [63:0] data);
      |import "DPI-C" function void mem_write(input [63:0] addr, input [3:0] len, input [63:0] data);
=======
    """import "DPI-C" function void mem_read(input [63:0] addr, output [63:0] data);
      |import "DPI-C" function void mem_write(input [63:0] addr, input [7:0] mask, input [63:0] data);
>>>>>>> npc:npc/playground/src/mem/BlackBoxMem.scala
      |module BlackBoxMem (
      |  input enable,
      |  input isRead,
      |  input [63:0] addr,
      |  input [7:0] mask,
      |  input [63:0] wdata,
      |  output [63:0] rdata,
      |  input clock
      |);
      |  wire read = enable & isRead;
      |/* verilator lint_off LATCH */
      |  always @(*) begin
<<<<<<< HEAD:npc/playground/src/BlackBoxMem.scala
      |    if(read&& !clock) mem_read(addr, len, rdata);
=======
      |    if(read&& !clock) mem_read(addr, rdata);
>>>>>>> npc:npc/playground/src/mem/BlackBoxMem.scala
      |  end
      |  wire write = enable & !isRead;
      |/* verilator lint_off LATCH */
      |  always @(*) begin
      |    if(write&& !clock) mem_write(addr, mask, wdata);  
      |  end
      |endmodule""".stripMargin
  )
}
