import circt.stage._

object Elaborate extends App {
  def top = new CPU()
  val generator = Seq(chisel3.stage.ChiselGeneratorAnnotation(() => top))
  for (val arg : args){
    print(arg)
  }
  print(args.toString())
  print("123")
  args :+ "-o=./build"
  (new ChiselStage).execute(args, generator :+ CIRCTTargetAnnotation(CIRCTTarget.Verilog))
}