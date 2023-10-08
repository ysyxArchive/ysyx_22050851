package utils

import chisel3._
<<<<<<< HEAD
import chisel3.experimental.ChiselEnum
import chisel3.util._
import firrtl.seqCat
import decode.Source
import decode.SourceType
=======
import chisel3.util._
import scala.collection.SeqFactory
import scala.collection.SeqOps
import chisel3.internal.Builder
import scala.collection.IterableFactory
import scala.annotation.unchecked.uncheckedVariance
>>>>>>> npc

object Utils {
  def signalExtend(num: UInt, length: Int): UInt = {
    Cat(Fill(64 - length, num(length - 1, length - 1)), num)
  }

<<<<<<< HEAD
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

=======
  def zeroExtend(num: UInt, width: Int, targetWidth: Int): UInt = {
    Cat(Fill(targetWidth - width, 0.U), num(width - 1, 0))
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
      ret := num
    }.otherwise {
      ret := 0.U
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
      ret := num
    }.otherwise {
      ret := 0.U
    }
    ret
  }
}

object EnumSeq {
  def apply[T <: Data](elems: (EnumType, T)*) = elems.map {
    case (enumType, uint) => (enumType.asUInt, uint)
  }
>>>>>>> npc
}

class FSM(val initState: UInt, val elems: List[(UInt, Bool, UInt)]) {
  val status = RegInit(initState)
  status := nextState(status)

  def nextState(current: UInt): UInt = {
    val table = elems.map({ case tri => (tri._1 === current && tri._2) -> tri._3 })
    MuxCase(current, table)
  }

  def trigger(from: UInt, to: UInt): Bool = {
    val table = elems.map({ case tri => (tri._1 === from && tri._3 === to) -> tri._2 })
    return status === from && MuxCase(false.B, table)
  }

  def willChangeTo(to: UInt): Bool = {
    val table = elems.map({ case tri => (tri._1 === status && tri._3 === to) -> tri._2 })
    return MuxCase(false.B, table)
  }

  def is(s: UInt): Bool = {
    return s === status
  }

  def willChange(): Bool = {
    elems.map({ case tri => (tri._1 === status && tri._2) }).reduce(_ || _)
  }
}
