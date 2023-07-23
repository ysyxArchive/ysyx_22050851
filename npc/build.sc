// import Mill dependency
import mill._
import mill.define.Sources
import mill.modules.Util
import scalalib._
// Hack
import publish._
// support BSP
import mill.bsp._
// input build.sc from each repositories.
import $file.dependencies.chisel.build
import $file.dependencies.cde.build
import $file.dependencies.`berkeley-hardfloat`.build
import $file.dependencies.`rocket-chip`.common

// Global Scala Version
object ivys {
  val sv = "2.13.10"
  val upickle = ivy"com.lihaoyi::upickle:1.3.15"
  val oslib = ivy"com.lihaoyi::os-lib:0.7.8"
  val pprint = ivy"com.lihaoyi::pprint:0.6.6"
  val utest = ivy"com.lihaoyi::utest:0.7.10"
  val jline = ivy"org.scala-lang.modules:scala-jline:2.12.1"
  val scalatest = ivy"org.scalatest::scalatest:3.2.2"
  val scalatestplus = ivy"org.scalatestplus::scalacheck-1-14:3.1.1.1"
  val scalacheck = ivy"org.scalacheck::scalacheck:1.14.3"
  val scopt = ivy"com.github.scopt::scopt:3.7.1"
  val playjson =ivy"com.typesafe.play::play-json:2.9.4"
  val breeze = ivy"org.scalanlp::breeze:1.1"
  val parallel = ivy"org.scala-lang.modules:scala-parallel-collections_3:1.0.4"
  val mainargs = ivy"com.lihaoyi::mainargs:0.4.0"
}

// Dummy

object playground extends CommonModule {
  override def moduleDeps = super.moduleDeps ++ Seq(myrocketchip, inclusivecache, blocks, shells)

  // add some scala ivy module you like here.
  override def ivyDeps = Agg(
    ivys.oslib,
    ivys.pprint,
    ivys.mainargs
  )

  def lazymodule: String = "freechips.rocketchip.system.ExampleRocketSystem"

  def configs: String = "playground.PlaygroundConfig"

  def elaborate = T {
    mill.modules.Jvm.runSubprocess(
      finalMainClass(),
      runClasspath().map(_.path),
      forkArgs(),
      forkEnv(),
      Seq(
        "--dir", T.dest.toString,
        "--lm", lazymodule,
        "--configs", configs
      ),
      workingDir = os.pwd,
    )
    PathRef(T.dest)
  }

  def verilog = T {
    os.proc("firtool",
      elaborate().path / s"${lazymodule.split('.').last}.fir",
      "-disable-infer-rw",
      "--disable-annotation-unknown",
      "-dedup",
      "-O=debug",
      "--split-verilog",
      "--preserve-values=named",
      "--output-annotation-file=mfc.anno.json",
      s"-o=${T.dest}"
    ).call(T.dest)
    PathRef(T.dest)
  }

}
