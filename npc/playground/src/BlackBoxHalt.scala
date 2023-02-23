import chisel3._
import chisel3.util.HasBlackBoxInline
class BlackBoxHalt extends BlackBox with HasBlackBoxInline {
  val io = IO(new Bundle {
    val halt = Input(Bool())
    val bad_halt = Input(Bool())
  })
  setInline(
    "BlackBoxHalt.v",
    """import "DPI-C" function void haltop(input is_good);
      |module BlackBoxHalt (
      |    input  halt,
      |    input  bad_halt
      |);
      |    wire is_halt = halt | bad_halt;
      |    wire is_good = halt & ~bad_halt;
      |    always @* begin
      |        if(is_halt)  haltop(is_good);
      |    end
      |endmodule""".stripMargin
  )
}
