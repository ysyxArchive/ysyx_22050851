package utils

import chisel3._
import chisel3.experimental.ChiselEnum
import chisel3.experimental.EnumType
import chisel3.util._
import firrtl.seqCat
import scala.collection.SeqFactory
import scala.collection.SeqOps
import chisel3.internal.Builder
import scala.collection.IterableFactory
import scala.annotation.unchecked.uncheckedVariance

object Utils {

  def cast(num: UInt, castWidth: Int, outputWidth: Int): UInt = {
    signExtend(num(castWidth - 1, 0), castWidth, outputWidth)
  }

  def zeroExtend(num: UInt, width: Int, targetWidth: Int): UInt = {
    Cat(Fill(targetWidth - num.getWidth, 0.U), num(width - 1, 0))
  }
  def zeroExtend(num: UInt, width: UInt): UInt = {
    val ret = Wire(UInt(64.W))
    when(width === 8.U) {
      ret := Utils.zeroExtend(num, 8, 64)
    }.elsewhen(width === 16.U) {
      ret := Utils.zeroExtend(num, 16, 64)
    }.elsewhen(width === 32.U) {
      ret := Utils.zeroExtend(num, 32, 64)
    }.elsewhen(width === 64.U) {
      ret := Utils.zeroExtend(num, 64, 64)
    }.otherwise {
      ret := 0.U
      throw new Error("width not implemented")
    }
    ret
  }
  def signExtend(num: UInt, width: Int, targetWidth: Int = 64): UInt = {
    Cat(Fill(targetWidth - width, num(width - 1)), num(width - 1, 0))
  }
  def signExtend(num: UInt, width: UInt): UInt = {
    val ret = Wire(UInt(64.W))
    when(width === 8.U) {
      ret := Utils.signExtend(num, 8, 64)
    }.elsewhen(width === 16.U) {
      ret := Utils.signExtend(num, 16, 64)
    }.elsewhen(width === 32.U) {
      ret := Utils.signExtend(num, 32, 64)
    }.elsewhen(width === 64.U) {
      ret := Utils.signExtend(num, 64, 64)
    }.otherwise {
      ret := 0.U
      throw new Error("width not implemented")
    }
    ret
  }
}

object EnumSeq {
  def apply(elems: (EnumType, UInt)*) = elems.map {
    case (enumType, uint) => (enumType.asUInt, uint)
  }
}
