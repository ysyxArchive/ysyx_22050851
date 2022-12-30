import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

import utest._

class ABasicTest extends AnyFlatSpec with ChiselScalatestTester {
  behavior.of("MyModule")
  it should "do something" in {
    test(new RegisterFile(32, 64)) { c =>
      c.io.wdata.poke(1.U)
      c.io.waddr.poke(1.U)
      c.io.wen.poke(true.B)
      c.io.raddr.poke(1.U)
      c.io.ren.poke(true.B)
      c.clock.step()
      c.io.rdata.expect(2.U)

    }
    // test body here
  }
}

// class RegisterFileSpec extends ChiselUtestTester {
//   val tests = Tests {
//     // test("RegisterFile") {
//     //   testCircuit(new RegisterFile(32, 64)) { c =>
//     test(new RegisterFile(32, 64)) { c =>
//       c.io.wdata.poke(1.U)
//       c.io.waddr.poke(1.U)
//       c.io.wen.poke(true.B)
//       c.io.raddr.poke(1.U)
//       c.io.ren.poke(true.B)
//       c.clock.step()
//       c.io.rdata.expect(2.U)

//     }
//   }

// }
