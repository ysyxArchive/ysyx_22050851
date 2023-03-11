package execute

import chisel3.experimental._

object ALUType extends ChiselEnum {
  val add, sub, and, or, xor, shiftLeft, shiftRightArth, shiftRightLogic = Value
}

object ALUSignalType extends ChiselEnum {
  val isZero, isNegative = Value
}
