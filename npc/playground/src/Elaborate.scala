import circt.stage._

object Elaborate extends App {
  println(args.mkString(", "))
  def top = new CPU()
  val generator = Seq(chisel3.stage.ChiselGeneratorAnnotation(() => top))
  (new ChiselStage).execute(args, generator :+ CIRCTTargetAnnotation(CIRCTTarget.Verilog))
}