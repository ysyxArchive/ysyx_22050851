// import Mill dependency
import mill._
import mill.scalalib._
import mill.scalalib.scalafmt.ScalafmtModule
import mill.scalalib.TestModule.Utest
// support BSP
import mill.bsp._

object playground extends ScalaModule with ScalafmtModule { m =>
  override def scalaVersion = "2.13.10"
  override def scalacOptions = Seq(
    "-language:reflectiveCalls",
    "-deprecation",
    "-feature",
    "-Xcheckinit"
  )
  override def ivyDeps = Agg(
    ivy"org.chipsalliance::chisel:5.0.0"
  )
  override def scalacPluginIvyDeps = Agg(
<<<<<<< HEAD
    ivy"edu.berkeley.cs:::chisel3-plugin:3.5.4",
=======
    ivy"org.chipsalliance:::chisel-plugin:5.0.0"
>>>>>>> npc
  )
  object test extends Tests with Utest {
    override def ivyDeps = m.ivyDeps() ++ Agg(
<<<<<<< HEAD
      ivy"com.lihaoyi::utest:0.7.10",
      ivy"edu.berkeley.cs::chiseltest:0.5.4",
=======
      ivy"com.lihaoyi::utest:0.8.1",
      ivy"edu.berkeley.cs::chiseltest:5.0.0"
>>>>>>> npc
    )
  }
  def repositoriesTask = T.task {
    Seq(
      coursier.MavenRepository("https://maven.aliyun.com/repository/central"),
      coursier.MavenRepository("https://repo.scala-sbt.org/scalasbt/maven-releases")
    ) ++ super.repositoriesTask()
  }
}
