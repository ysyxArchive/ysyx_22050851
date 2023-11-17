import chisel3._
import chisel3.util.HasBlackBoxInline

class CacheDebugIO(
  val wayCnt:    Int = 4,
  val groupSize: Int = 4)
    extends Bundle {
  val cacheStatus =
    Vec(
      wayCnt,
      Vec(
        groupSize,
        new Bundle {
          val valid = Bool()
          val dirty = Bool()
        }
      )
    )
  val changed = Bool()
}

class BlackBoxCache(wayCnt: Int = 4, groupSize: Int = 4) extends BlackBox with HasBlackBoxInline {
  val io = IO(new CacheDebugIO(wayCnt, groupSize));
  setInline(
    "BlackBoxCache.v",
    """import "DPI-C" function void cache_change();
        import "DPI-C" function void set_cacheinfo_ptr(input logic [63:0] d [], input logic [63:0] v []);
       |module BlackBoxCache (
       |  input changed,
       """ +
      Seq
        .tabulate(wayCnt)(i =>
          Seq
            .tabulate(groupSize)(j => s"| input cacheStatus_${i}_${j}_valid\n| input cacheStatus_${i}_${j}_dirty\n")
            .reduce(_ ++ _)
        )
        .reduce(_ ++ _) +
      s"""|  input clock
          |);
          |  wire valids [0:${wayCnt * groupSize - 1}];
          |  wire dirty [0:${wayCnt * groupSize - 1}];
      """ + Seq
      .tabulate(wayCnt)(i =>
        Seq
          .tabulate(groupSize)(j =>
            s"| assign valid[${i * groupSize + j}] = cacheStatus_${i}_${j}_valid\n| assign dirty[${i * groupSize + j}] cacheStatus_${i}_${j}_dirty\n"
          )
          .reduce(_ ++ _)
      )
      .reduce(_ ++ _) +
      """  
        |  always @(posedge clock) begin
        |    if(changed) cache_change();
        |  end
        |  initial set_cacheinfo_ptr(dirty, valids);
        |endmodule""".stripMargin
  )
}
