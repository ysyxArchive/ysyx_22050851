import chisel3._
import chisel3.util.HasBlackBoxInline

class PiplineDebugIO extends Bundle {
  val ifHalt  = Bool()
  val idHalt  = Bool()
  val exHalt  = Bool()
  val memHalt = Bool()
  val wbHalt  = Bool()
  val clock   = Clock()
}

class BlackBoxPip extends BlackBox with HasBlackBoxInline {
  val io = IO(Flipped(new PiplineDebugIO()));
  setInline(
    "BlackBoxPip.v",
    ("""import "DPI-C" function void pip_info(input logic ifWait, input logic idWait, input logic exWait, input logic memWait, input logic wbWait);
       |module BlackBoxPip (
       |  input ifHalt,
       |  input idHalt,
       |  input exHalt,
       |  input memHalt,
       |  input wbHalt,
       |  input clock
       |);
       |  always @(posedge clock) begin
       |    pip_info(ifHalt, idHalt, exHalt, memHalt, wbHalt);
       |  end
       |    
       |endmodule""").stripMargin
  )
}
