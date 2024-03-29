import chisel3._
import chisel3.Input
import chiseltest._
import chisel3.experimental.BundleLiterals._
import utest._
import os.truncate
import org.scalatest._
import org.scalatest.flatspec._
import org.scalatest._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import execute.SimpleDivider

/**
  * This is a trivial example of how to run this Specification
  * From within sbt use:
  * {{{
  * testOnly gcd.GcdDecoupledTester
  * }}}
  * From a terminal shell use:
  * {{{
  * sbt 'testOnly gcd.GcdDecoupledTester'
  * }}}
  */

// object Utils {
//   def initIO(m:

// , io: Data) {
//     println(s"${io.specifiedDirection} ${isInput(io.input1)}")
//     // // Example usage
//     // println(s"input1 is an input? ${isInput(io.input1)}")
//     // println(s"output1 is an input? ${isInput(io.output1)}")
//   }

//   def isInputOrOutput(signal: Data): String = {
//     val parentModule = signal.topBindingOption.flatMap(_.bindingOption).map(_.owner)
//     parentModule match {
//       case Some(module: RawModule) =>
//         if (module.outputPorts.contains(signal)) "Output"
//         else if (module.inputPorts.contains(signal)) "Input"
//         else "Unknown"
//       case _ => "Unknown"
//     }
//   }
// }

object DivSpec extends ChiselUtestTester {
  val tests = Tests {
    // test("Divider flush") {
    //   testCircuit(
    //     new SimpleMultiplier(),
    //     Seq(WriteVcdAnnotation)
    //   ) { dut =>
    //     {
    //       dut.io.flush.poke(false.B)
    //       dut.io.mulw.poke(false.B)
    //       dut.io.mulSigned.poke(0.U)

    //       val random   = new scala.util.Random()
    //       val a        = BigInt(64, random)
    //       val b        = BigInt(64, random)
    //       val expected = a * b

    //       dut.io.multiplicand.poke(a.U)
    //       dut.io.multiplier.poke(b.U)
    //       dut.io.mulValid.poke(true.B)
    //       while (dut.io.mulReady.peekInt() == 0) {
    //         dut.clock.step(1)
    //       }
    //       dut.clock.step(1)
    //       dut.io.mulValid.poke(false.B)
    //       dut.clock.step(10)
    //       dut.io.flush.poke(true.B)
    //       dut.clock.step(1)
    //       dut.io.flush.poke(false.B)
    //       dut.clock.step(1)
    //       dut.io.outValid.expect(false.B)
    //       dut.io.mulReady.expect(true.B)
    //     }
    //   }
    // }
    test("Divider base") {
      testCircuit(
        new SimpleDivider(),
        Seq(WriteVcdAnnotation)
      ) { dut =>
        {
          dut.io.flush.poke(false.B)
          dut.io.divw.poke(false.B)
          dut.io.divSigned.poke(0.U)

          val random         = new scala.util.Random()
          val a              = BigInt(63, random)
          val b              = BigInt(63, random)
          val expectedQuo    = a / b
          val expectedRemind = a % b

          dut.io.dividend.poke(a.U)
          dut.io.divisor.poke(b.U)
          dut.io.divValid.poke(true.B)
          while (dut.io.divReady.peekInt() == 0) {
            dut.clock.step(1)
          }
          dut.clock.step(1)
          dut.io.divValid.poke(false.B)

          while (dut.io.outValid.peekInt() == 0) {
            dut.clock.step(1)
          }
          dut.io.quotient.expect(expectedQuo)
          dut.io.remainder.expect(expectedRemind)
        }
      }
    }

    test("Divider small") {
      testCircuit(
        new SimpleDivider(),
        Seq(WriteVcdAnnotation)
      ) { dut =>
        {
          dut.io.flush.poke(false.B)
          dut.io.divw.poke(false.B)
          dut.io.divSigned.poke(0.U)

          val random         = new scala.util.Random()
          val a              = BigInt(63, random)
          val b              = BigInt(15, random)
          val expectedQuo    = a / b
          val expectedRemind = a % b

          dut.io.dividend.poke(a.U)
          dut.io.divisor.poke(b.U)
          dut.io.divValid.poke(true.B)
          while (dut.io.divReady.peekInt() == 0) {
            dut.clock.step(1)
          }
          dut.clock.step(1)
          dut.io.divValid.poke(false.B)

          while (dut.io.outValid.peekInt() == 0) {
            dut.clock.step(1)
          }
          dut.io.quotient.expect(expectedQuo)
          dut.io.remainder.expect(expectedRemind)
        }
      }
    }

  }
}
