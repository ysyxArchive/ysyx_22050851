import chisel3._
import chisel3.util.DecoupledIO
// AXI5-Lite

class AxiLiteWriteRequest(addr_width: Int, id_w_width: Int = 1) extends Bundle {
  val id   = Output(UInt(id_w_width.W)) // YS ID_W_WIDTH > 0
  val addr = Output(UInt(addr_width.W)) // Y -
  val prot = Output(UInt(3.W)) // YM PROT_Present
  //   val size     = Input(Bool()) // O SIZE_Present
  //   val user     = Input(Bool()) // O USER_REQ_WIDTH > 0
  //   val trace    = Input(Bool()) // O Trace_Signals
  //   val subsysid = Input(Bool()) // O SUBSYSID_WIDTH > 0
  //   val idunq    = Input(Bool()) // O Unique_ID_Support
  //   val akeup    = Input(Bool()) // O Wakeup_Signals

}

class AxiLiteWriteData(dataType: Data) extends Bundle {
  val data = Output(dataType) // Y -
  val strb = Output(UInt((dataType.getWidth / 8).W)) // YS WSTRB_Present
  //   val user      = Input(Bool()) // O USER_DATA_WIDTH > 0
  //   val poison    = Input(Bool()) // O Poison
  //   val trace     = Input(Bool()) // O Trace_Signals
}

class AxiLiteWriteResponse(id_w_width: Int = 1) extends Bundle {
  val id = Output(UInt(id_w_width.W)) // YS ID_W_WIDTH > 0
  //   val BRESP      = Input(Bool()) // O BRESP_WIDTH > 0
  //   val BUSER      = Input(Bool()) // O USER_RESP_WIDTH > 0
  //   val BTRACE     = Input(Bool()) // O Trace_Signals
  //   val BIDUNQ     = Input(Bool()) // O Unique_ID_Support
}

class AxiLiteReadRequest(addr_width: Int, id_r_width: Int = 1) extends Bundle {
  val id   = Output(UInt(id_r_width.W)) // YS ID_R_WIDTH > 0
  val addr = Output(UInt(addr_width.W)) // Y -
  val prot = Output(UInt(3.W)) // YM PROT_Present
  //   val size     = Input(Bool()) // O SIZE_Present
  //   val user     = Input(Bool()) // O USER_REQ_WIDTH > 0
  //   val trace    = Input(Bool()) // O Trace_Signals
  //   val subsysid = Input(Bool()) // O SUBSYSID_WIDTH > 0
  //   val idunq    = Input(Bool()) // O Unique_ID_Support

}

class AxiLiteReadData(dataType: Data, id_r_width: Int = 1) extends Bundle {
  val id   = Output(UInt(id_r_width.W)) // YS ID_R_WIDTH > 0
  val data = Output(dataType) // Y -
  //   val resp      = Input(Bool()) // O RRESP_WIDTH > 0
  //   val user      = Input(Bool()) // O USER_DATA_WIDTH > 0 orUSER_RESP_WIDTH >
  //   val poison    = Input(Bool()) // O Poison
  //   val trace     = Input(Bool()) // O Trace_Signals
  //   val idunq     = Input(Bool()) // O Unique_ID_Support

}
class AxiLiteIO(dataType: Data, addr_width: Int) extends Bundle {
  val AW = DecoupledIO(new AxiLiteWriteRequest(addr_width))
  val W  = DecoupledIO(new AxiLiteWriteData(dataType))
  val B  = Flipped(DecoupledIO(new AxiLiteWriteResponse()))
  val AR = DecoupledIO(new AxiLiteReadRequest(addr_width))
  val R  = Flipped(DecoupledIO(new AxiLiteReadData(dataType)))
}

object AxiLiteIO {
  def apply(dataWidth: Int, addr_width:  Int = 64) = new AxiLiteIO(UInt(dataWidth.W), addr_width)
  def apply(dataType:  Data, addr_width: Int = 64) = new AxiLiteIO(dataType, addr_width)
}

// class AxiLiteReadIO(data_width: Int = 64, addr_width: Int = 64) extends Bundle {
//   val AR = DecoupledIO(new AxiLiteReadRequest(addr_width))
//   val R  = Flipped(DecoupledIO(new AxiLiteReadData(data_width)))

// }

// class AxiLiteWriteIO(data_width: Int = 64, addr_width: Int = 64) extends Bundle {
//   val AW = DecoupledIO(new AxiLiteWriteRequest(addr_width))
//   val W  = DecoupledIO(new AxiLiteWriteData(data_width))
//   val B  = Flipped(DecoupledIO(new AxiLiteWriteResponse()))
// }
