import circt.stage._

object Elaborate extends App {
  def top = new CPU()
  val generator = Seq(chisel3.stage.ChiselGeneratorAnnotation(() => top))
  args :+ "123"
  (new ChiselStage).execute(args, generator :+ CIRCTTargetAnnotation(CIRCTTarget.Verilog))
}