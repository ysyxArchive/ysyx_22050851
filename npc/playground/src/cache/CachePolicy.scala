import chisel3._
import chisel3.util._

/**
  * @param wayCnt 路数
  * @param groupSize 单路单元数
  */
class CachePolicyIO(dataWidth: Int, groupSize: Int) extends Bundle {
  val update       = Input(Bool())
  val hit          = Input(Bool())
  val hitIndex     = Input(UInt(log2Ceil(groupSize).W))
  val replaceIndex = Output(UInt(log2Ceil(groupSize).W))
}

class NaiveCachePolicy(dataWidth: Int, groupSize: Int) extends Module {
  val io           = IO(new CachePolicyIO(dataWidth, groupSize))
  val replaceIndex = RegInit(0.U(log2Ceil(groupSize).W))
  replaceIndex := Mux(
    io.update,
    Mux(replaceIndex === (groupSize - 1).U, 0.U, replaceIndex + 1.U),
    replaceIndex
  )
  io.replaceIndex := replaceIndex
}

class PLRUCachePolicy(dataWidth: Int, groupSize: Int) extends Module {
  val pointerLayer = log2Ceil(groupSize)

  val io           = IO(new CachePolicyIO(dataWidth, groupSize))
  val replaceIndex = Wire(Vec(pointerLayer, Bool()))

  val pointers = Reg(Vec(pointerLayer, Vec(pointerLayer, Bool())))

  val hitIndexReverse = Reverse(io.hitIndex)

  when(io.update && io.hit) {
    for (i <- 0 until pointerLayer) {
      pointers(i)(io.hitIndex >> (pointerLayer - i)) := io.hitIndex >> pointerLayer - i - 1
    }
  }
  for (i <- 0 until pointerLayer) {
    if (i == 0) {
      replaceIndex(pointerLayer - 1) := !pointers(0)(0)
    } else {
      replaceIndex(pointerLayer - i - 1) := !pointers(i)(
        VecInit(replaceIndex.slice(pointerLayer - i, pointerLayer)).asUInt
      )
    }
  }
  io.replaceIndex := replaceIndex.asUInt

}
