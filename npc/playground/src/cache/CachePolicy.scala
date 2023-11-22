import chisel3._
import chisel3.util._
import decode.InstType

/**
  * @param wayCnt 路数
  * @param groupSize 单路单元数
  */
class CachePolicyIO(dataWidth: Int, groupSize: Int) extends Bundle {
  val fire         = Input(Bool())
  val hit          = Input(Bool())
  val hitIndex     = Input(UInt(log2Ceil(groupSize).W))
  val replaceIndex = Output(UInt(log2Ceil(groupSize).W))
}

class NaiveCachePolicy(dataWidth: Int, groupSize: Int) extends Module {
  val io           = IO(new CachePolicyIO(dataWidth, groupSize))
  val replaceIndex = RegInit(0.U(log2Ceil(groupSize).W))
  replaceIndex := Mux(
    io.fire,
    Mux(replaceIndex === (groupSize - 1).U, 0.U, replaceIndex + 1.U),
    replaceIndex
  )
  val out = RegInit(0.U(log2Ceil(groupSize).W))
  out             := Mux(io.fire, replaceIndex, out)
  
  io.replaceIndex := out
}
