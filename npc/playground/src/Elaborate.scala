import circt.stage._

object Elaborate extends App {
  println(args.mkString(", "))
  var filteredArgs     = args.clone()
  val isDebug          = args.contains("--debug")
  val shouldRemoveDPIC = args.contains("--rm-dpic")
  if (isDebug) {
    filteredArgs = args.filter(_ != "--debug")
  }
  if (shouldRemoveDPIC) {
    filteredArgs = args.filter(_ != "--rm-dpic")
  }
  def top       = new CPU(isDebug)
  val generator = Seq(chisel3.stage.ChiselGeneratorAnnotation(() => top))
  (new ChiselStage).execute(filteredArgs, generator :+ CIRCTTargetAnnotation(CIRCTTarget.Verilog))
}
