import chisel3._
import chiseltest._
import utest._

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
object RegisterFileSpec extends ChiselUtestTester {
  val tests = Tests {
    test("RegisterFile") {
      testCircuit(new RegisterFile()) {
        dut =>
          dut.io.waddr.poke(1.U)
          dut.io.raddr1.poke(1.U)
          dut.io.wdata.poke(2.U)
          dut.io.regWrite.poke(true.B)
          dut.io.out1.expect(2.U)
          dut.clock.step()
          dut.io.waddr.poke(2.U)
          dut.io.raddr1.poke(1.U)
          dut.io.wdata.poke(3.U)
          dut.io.regWrite.poke(true.B)
          dut.io.out1.expect(2.U)
          dut.clock.step()
          dut.io.waddr.poke(1.U)
          dut.io.raddr1.poke(1.U)
          dut.io.wdata.poke(3.U)
          dut.io.regWrite.poke(false.B)
          dut.io.out1.expect(2.U)
          dut.clock.step()
          dut.io.waddr.poke(0.U)
          dut.io.raddr1.poke(0.U)
          dut.io.wdata.poke(3.U)
          dut.io.regWrite.poke(true.B)
          dut.io.out1.expect(0.U)
      }
    }
  }
}
