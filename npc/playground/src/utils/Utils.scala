package utils

import chisel3._
import chisel3.experimental.ChiselEnum
import chisel3.util._
import firrtl.seqCat
import decode.Source
import decode.SourceType

object Utils {
  def signalExtend(num: UInt, length: Int): UInt = {
    Cat(Fill(64 - length, num(length - 1, length - 1)), num)
  }

  def signalExtend(num: UInt, bytelength: UInt): UInt = {
    val signal = MuxLookup(
      bytelength,
      false.B,
      Seq(
        (1.U) -> num(7),
        (2.U) -> num(15),
        (4.U) -> num(31),
        (8.U) -> num(63)
      )
    )
    val mask = Fill(8, signal)
    Cat(
      Mux(bytelength > 4.U, num(63, 56), mask),
      Mux(bytelength > 4.U, num(55, 48), mask),
      Mux(bytelength > 4.U, num(47, 40), mask),
      Mux(bytelength > 4.U, num(39, 32), mask),
      Mux(bytelength > 2.U, num(31, 24), mask),
      Mux(bytelength > 2.U, num(23, 16), mask),
      Mux(bytelength > 1.U, num(15, 8), mask),
      num(7, 0)
    )
  }

  def isRegType(source: Source): Bool =
    source.stype === SourceType.reg.asUInt || source.stype === SourceType.regLow.asUInt

}
