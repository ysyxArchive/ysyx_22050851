import circt.stage._

object Elaborate extends App {
  println(args.mkString(", "))
<<<<<<< HEAD
  var filteredArgs = args.clone()
  val isDebug      = args.contains("--debug")
  if (isDebug) {
    filteredArgs = args.filter(_ != "--debug")
  }
  def top       = new CPU(isDebug)
=======
  var filteredArgs     = args.clone()
  val isDebug          = args.contains("--debug")
  val shouldRemoveDPIC = args.contains("--rm-dpic")
  if (isDebug) {
    filteredArgs = args.filter(_ != "--debug")
  }
  if (shouldRemoveDPIC) {
    filteredArgs = args.filter(_ != "--rm-dpic")
  }
  def top       = new CPU(isDebug, shouldRemoveDPIC)
>>>>>>> adaab1e8590675071c22ec50f610816123747f3a
  val generator = Seq(chisel3.stage.ChiselGeneratorAnnotation(() => top))
  (new ChiselStage).execute(filteredArgs, generator :+ CIRCTTargetAnnotation(CIRCTTarget.Verilog))
}
