package utils

import chisel3._
import chisel3.experimental.ChiselEnum
import chisel3.util._
import firrtl.seqCat
import decode.Source
import decode.SourceType
import execute.ALUUtils

object Utils {

  def cast(num: UInt, castWidth: Int, outputWidth: Int): UInt = {
    signExtend(num(castWidth - 1, 0), castWidth, outputWidth)
  }

  def zeroExtend(num: UInt, width: Int, targetWidth: Int): UInt = {
    Cat(Fill(targetWidth - num.getWidth, 0.U), num(width - 1, 0))
  }

  def signExtend(num: UInt, width: Int, targetWidth: Int = 64): UInt = {
    Cat(Fill(targetWidth - width, num(width - 1)), num(width - 1, 0))
  }

  def isRegType(source: Source): Bool =
    source.stype === SourceType.reg.asUInt || source.stype === SourceType.regLow.asUInt

}
