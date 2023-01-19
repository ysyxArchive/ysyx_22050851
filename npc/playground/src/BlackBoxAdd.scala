import chisel3._
import chisel3.util.HasBlackBoxInline
class BlackBoxAdd extends BlackBox with HasBlackBoxInline {
  val io = IO(new Bundle {
    val halt = Input(Bool())
  })
  setInline(
    "BlackBoxAdd.v",
    """import "DPI-C" function void halt();
      |module BlackBoxAdd(
      |    input  halt
      |);
      |    always @* begin
      |        if(halt)  halt();
      |    end
      |endmodule""".stripMargin
  )
}
