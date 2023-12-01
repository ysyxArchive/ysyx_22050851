import circt.stage._

object Elaborate extends App {
  println(args.mkString(", "))
  var filteredArgs = args.clone()
  val isDebug      = args.contains("--debug")
  if (isDebug) {
    filteredArgs = args.filter(_ != "--debug")
  }
  def top       = new CPU(isDebug)
  val generator = Seq(chisel3.stage.ChiselGeneratorAnnotation(() => top))
  (new ChiselStage).execute(filteredArgs, generator :+ CIRCTTargetAnnotation(CIRCTTarget.Verilog))
}
