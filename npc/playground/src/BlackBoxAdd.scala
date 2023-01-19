import chisel3._
import chisel3.util.HasBlackBoxInline
class BlackBoxAdd extends BlackBox with HasBlackBoxInline {
  val io = IO(new Bundle {
    val halt = Input(Bool())
  })
  setInline(
    "BlackBoxAdd.v",
    """module BlackBoxAdd(
      |    input  halt,
      |);
      |always @* begin
      |  out <= $realtobits($bitstoreal(in1) + $bitstoreal(in2));
      |end
      |endmodule
    """.stripMargin
  )
}
