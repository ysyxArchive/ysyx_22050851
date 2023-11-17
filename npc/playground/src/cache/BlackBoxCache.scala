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
  val changed  = Bool()
  val clock    = Clock()
  val isDCache = Bool()
}

class BlackBoxCache(wayCnt: Int = 4, groupSize: Int = 4) extends BlackBox with HasBlackBoxInline {
  val io = IO(Flipped(new CacheDebugIO(wayCnt, groupSize)));
  setInline(
    "BlackBoxCache.v",
    (s"""import "DPI-C" function void cache_change(input logic isDCache, input logic d [0:15], input logic v [0:15]);
       |module BlackBoxCache (
       |  input isDCache,
       |  input changed,
       """ +
      Seq
        .tabulate(wayCnt)(i =>
          Seq
            .tabulate(groupSize)(j => s"| input cacheStatus_${i}_${j}_valid,\n| input cacheStatus_${i}_${j}_dirty,\n")
            .reduce(_ ++ _)
        )
        .reduce(_ ++ _) +
      s"""|  input clock
          |);
          |  wire valid [0:${wayCnt * groupSize - 1}];
          |  wire dirty [0:${wayCnt * groupSize - 1}];
      """ + Seq
      .tabulate(wayCnt)(i =>
        Seq
          .tabulate(groupSize)(j =>
            s"| assign valid[${i * groupSize + j}] = cacheStatus_${i}_${j}_valid;\n| assign dirty[${i * groupSize + j}] = cacheStatus_${i}_${j}_dirty;\n"
          )
          .reduce(_ ++ _)
      )
      .reduce(_ ++ _) +
      s"""  
         |  always @(posedge clock) begin
         |    if(changed) cache_change(isDCache, dirty,valid);
         |  end
         |endmodule""").stripMargin
  )
}
