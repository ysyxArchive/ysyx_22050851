// import Mill dependency
import mill._
import mill.scalalib._
import mill.scalalib.scalafmt.ScalafmtModule
import mill.scalalib.TestModule.Utest
// support BSP
import mill.bsp._
import coursier.maven.MavenRepository

object CustomZincWorkerModule extends ZincWorkerModule {
  def repositories() = super.repositories ++ Seq(
    MavenRepository("https://oss.sonatype.org/content/repositories/releases"),
    MavenRepository("https://oss.sonatype.org/content/repositories/snapshots")
  )  
}

object playground extends ScalaModule with ScalafmtModule { m =>
  override def scalaVersion = "2.13.8"
  override def scalacOptions = Seq(
    "-language:reflectiveCalls",
    "-deprecation",
    "-feature",
    "-Xcheckinit",
    "-P:chiselplugin:genBundleElements"
  )
  override def ivyDeps = Agg(
    ivy"edu.berkeley.cs::chisel3:3.5.4",
    ivy"com.sifive::chisel-circt:0.6.0",
  )
  override def scalacPluginIvyDeps = Agg(
    ivy"edu.berkeley.cs:::chisel3-plugin:3.5.4",
  )
  object test extends Tests with Utest {
    override def ivyDeps = m.ivyDeps() ++ Agg(
      ivy"com.lihaoyi::utest:0.7.10",
      ivy"edu.berkeley.cs::chiseltest:0.5.4",
    )
  }
}
