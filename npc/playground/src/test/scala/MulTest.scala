package chiseltest.tests
import chisel3._
import chisel3.Input
import chiseltest._
import chisel3.experimental.BundleLiterals._
import execute.SimpleMultiplier
import os.truncate
import org.scalatest._
import org.scalatest.flatspec._
import org.scalatest._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

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

// object MulSpec extends ChiselUtestTester {
//   val tests = Tests {

//     test("Multiplier base") {
//       testCircuit(
//         new SimpleMultiplier(),
//         Seq(WriteVcdAnnotation, VerilatorBackendAnnotation)
//       ) { dut =>
//         dut.io.flush.poke(false.B)
//         dut.io.mulw.poke(false.B)
//         dut.io.mulSigned.poke(0.U)

//         val random   = new scala.util.Random()
//         val a        = BigInt(64, random)
//         val b        = BigInt(64, random)
//         val expected = a * b

//         dut.io.multiplicand.poke(a.U)
//         dut.io.multiplier.poke(b.U)
//         dut.io.mulValid.poke(true.B)
//         while (dut.io.mulReady.peekInt() == 0) {
//           dut.clock.step(1)
//         }
//         dut.clock.step(1)
//         dut.io.mulValid.poke(false.B)

//         while (dut.io.outValid.peekInt() == 0) {
//           dut.clock.step(1)
//         }
//         dut.io.resultLow.expect(expected.U(63, 0))
//         dut.io.resultHigh.expect(expected.U(128, 64))
//       }
//     }
//     test("Multiplier flush") {
//       testCircuit(
//         new SimpleMultiplier(),
//         Seq(WriteVcdAnnotation, VerilatorBackendAnnotation)
//       ) { dut =>
//         dut.io.flush.poke(false.B)
//         dut.io.mulw.poke(false.B)
//         dut.io.mulSigned.poke(0.U)

//         val random   = new scala.util.Random()
//         val a        = BigInt(64, random)
//         val b        = BigInt(64, random)
//         val expected = a * b

//         dut.io.multiplicand.poke(a.U)
//         dut.io.multiplier.poke(b.U)
//         dut.io.mulValid.poke(true.B)
//         while (dut.io.mulReady.peekInt() == 0) {
//           dut.clock.step(1)
//         }
//         dut.clock.step(1)
//         dut.io.mulValid.poke(false.B)
//         dut.clock.step(10)
//         dut.io.flush.poke(true.B)
//         dut.clock.step(1)
//         dut.io.flush.poke(false.B)
//         dut.clock.step(1)
//         dut.io.outValid.expect(false.B)
//         dut.io.mulReady.expect(true.B)
//       }
//     }
//   }
// }

class MulTest extends AnyFlatSpec with ChiselScalatestTester with Matchers {
  behavior.of("SimpleMultiplier")

  it should "test static circuits" in {

    test(new SimpleMultiplier()) { dut =>
      dut.io.flush.poke(false.B)
      dut.io.mulw.poke(false.B)
      dut.io.mulSigned.poke(0.U)

      val random   = new scala.util.Random()
      val a        = BigInt(64, random)
      val b        = BigInt(64, random)
      val expected = a * b

      dut.io.multiplicand.poke(a.U)
      dut.io.multiplier.poke(b.U)
      dut.io.mulValid.poke(true.B)
      while (dut.io.mulReady.peekInt() == 0) {
        dut.clock.step(1)
      }
      dut.clock.step(1)
      dut.io.mulValid.poke(false.B)

      while (dut.io.outValid.peekInt() == 0) {
        dut.clock.step(1)
      }
      dut.io.resultLow.expect(expected.U(63, 0))
      dut.io.resultHigh.expect(expected.U(128, 64))
    }
  }

  // it should "test static circuits" in {
  //   test(new StaticModule(42.U)) { c =>
  //     c.out.expect(42.U)
  //   }
  // }

  // it should "fail on poking outputs" in {
  //   assertThrows[UnpokeableException] {
  //     test(new StaticModule(42.U)) { c =>
  //       c.out.poke(0.U)
  //     }
  //   }
  // }

  // it should "fail on peeking internal signals" in {
  //   val e = intercept[UnpeekableException] {
  //     test(new Module {
  //       val hidden = Reg(Bool())
  //     }) { c =>
  //       c.hidden.peek()
  //     }
  //   }
  //   assert(e.getMessage.contains("hidden"))
  // }

  // it should "fail on peeking internal signals with single threaded backend" in {
  //   val e = intercept[UnpeekableException] {
  //     test(new Module {
  //       val hidden = Reg(Bool())
  //     }) { c =>
  //       c.hidden.peek()
  //     }
  //   }
  //   assert(e.getMessage.contains("hidden"))
  // }

  // it should "fail on expect mismatch" in {
  //   assertThrows[exceptions.TestFailedException] {
  //     test(new StaticModule(42.U)) { c =>
  //       c.out.expect(0.U)
  //     }
  //   }
  // }

  // it should "test record partial poke" in {
  //   val typ = new CustomBundle("foo" -> UInt(32.W), "bar" -> UInt(32.W))
  //   test(new PassthroughModule(typ)) { c =>
  //     c.in.pokePartial(
  //       typ.Lit(
  //         _.elements("foo") -> 4.U
  //       )
  //     )
  //     c.out.expectPartial(
  //       typ.Lit(
  //         _.elements("foo") -> 4.U
  //       )
  //     )
  //     c.in.pokePartial(
  //       typ.Lit(
  //         _.elements("bar") -> 5.U
  //       )
  //     )
  //     c.out.expect(
  //       typ.Lit(
  //         _.elements("foo") -> 4.U,
  //         _.elements("bar") -> 5.U
  //       )
  //     )
  //   }
  // }

  // it should "fail on record expect mismatch" in {
  //   val typ = new CustomBundle("foo" -> UInt(32.W), "bar" -> UInt(32.W))
  //   assertThrows[exceptions.TestFailedException] {
  //     test(new PassthroughModule(typ)) { c =>
  //       c.in.pokePartial(
  //         typ.Lit(
  //           _.elements("foo") -> 4.U
  //         )
  //       )
  //       c.out.expect(
  //         typ.Lit(
  //           _.elements("foo") -> 4.U,
  //           _.elements("bar") -> 5.U
  //         )
  //       )
  //     }
  //   }
  // }

  // it should "fail on partial expect mismatch" in {
  //   val typ = new CustomBundle("foo" -> UInt(32.W), "bar" -> UInt(32.W))
  //   assertThrows[exceptions.TestFailedException] {
  //     test(new PassthroughModule(typ)) { c =>
  //       c.in.poke(
  //         typ.Lit(
  //           _.elements("foo") -> 4.U,
  //           _.elements("bar") -> 5.U
  //         )
  //       )
  //       c.out.expectPartial(
  //         typ.Lit(
  //           _.elements("foo") -> 5.U
  //         )
  //       )
  //     }
  //   }
  // }

  // it should "fail with user-defined message" in {
  //   intercept[exceptions.TestFailedException] {
  //     test(new StaticModule(42.U)) { c =>
  //       c.out.expect(0.U, "user-defined failure message =(")
  //     }
  //   }.getMessage should include("user-defined failure message =(")
  // }

  // it should "test inputless sequential circuits" in {
  //   test(new Module {
  //     val io = IO(new Bundle {
  //       val out = Output(UInt(8.W))
  //     })
  //     val counter = RegInit(UInt(8.W), 0.U)
  //     counter := counter + 1.U
  //     io.out  := counter
  //   }) { c =>
  //     c.io.out.expect(0.U)
  //     c.clock.step()
  //     c.io.out.expect(1.U)
  //     c.clock.step()
  //     c.io.out.expect(2.U)
  //     c.clock.step()
  //     c.io.out.expect(3.U)
  //   }
  // }

  // it should "test combinational circuits" in {
  //   test(new PassthroughModule(UInt(8.W))) { c =>
  //     c.in.poke(0.U)
  //     c.out.expect(0.U)
  //     c.in.poke(42.U)
  //     c.out.expect(42.U)
  //   }
  // }

  // it should "test sequential circuits" in {
  //   test(new Module {
  //     val io = IO(new Bundle {
  //       val in  = Input(UInt(8.W))
  //       val out = Output(UInt(8.W))
  //     })
  //     io.out := RegNext(io.in, 0.U)
  //   }) { c =>
  //     c.io.in.poke(0.U)
  //     c.clock.step()
  //     c.io.out.expect(0.U)
  //     c.io.in.poke(42.U)
  //     c.clock.step()
  //     c.io.out.expect(42.U)
  //   }
  // }

  // it should "test reset" in {
  //   test(new Module {
  //     val io = IO(new Bundle {
  //       val in  = Input(UInt(8.W))
  //       val out = Output(UInt(8.W))
  //     })
  //     io.out := RegNext(io.in, 0.U)
  //   }) { c =>
  //     c.io.out.expect(0.U)

  //     c.io.in.poke(42.U)
  //     c.clock.step()
  //     c.io.out.expect(42.U)

  //     c.reset.poke(true.B)
  //     c.io.out.expect(42.U) // sync reset not effective until next clk
  //     c.clock.step()
  //     c.io.out.expect(0.U)

  //     c.clock.step()
  //     c.io.out.expect(0.U)

  //     c.reset.poke(false.B)
  //     c.io.in.poke(43.U)
  //     c.clock.step()
  //     c.io.out.expect(43.U)
  //   }
  // }
}
