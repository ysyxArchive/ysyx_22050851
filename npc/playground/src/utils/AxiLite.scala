import chisel3._
import chisel3.util._
import utils.FSM
import scala.math._
import decode.CsrSetMode

object BurstType extends ChiselEnum {
  val FIXED = Value(0.U)
  val INCR  = Value(1.U)
  val WRAP  = Value(2.U)
}

class AxiLiteWriteRequest(addr_width: Int, id_w_width: Int) extends Bundle {
  val id   = Output(UInt(id_w_width.W)) // YS ID_W_WIDTH > 0
  val addr = Output(UInt(addr_width.W)) // Y -
  val prot = Output(UInt(3.W)) // YM PROT_Present
  // val size = Input(UInt(3.W)) // O SIZE_Present
  //   val user     = Input(Bool()) // O USER_REQ_WIDTH > 0
  //   val trace    = Input(Bool()) // O Trace_Signals
  //   val subsysid = Input(Bool()) // O SUBSYSID_WIDTH > 0
  //   val idunq    = Input(Bool()) // O Unique_ID_Support
  //   val akeup    = Input(Bool()) // O Wakeup_Signals
}

class BurstLiteWriteRequest(addr_width: Int, id_w_width: Int) extends AxiLiteWriteRequest(addr_width, id_w_width) {
  val burst = Output(UInt(BurstType.getWidth.W))
  val len   = Output(UInt(8.W))
}

class AxiLiteWriteData(dataType: Data) extends Bundle {
  val data = Output(dataType) // Y -
  val strb = Output(UInt((dataType.getWidth / 8).W)) // YS WSTRB_Present
  //   val user      = Input(Bool()) // O USER_DATA_WIDTH > 0
  //   val poison    = Input(Bool()) // O Poison
  //   val trace     = Input(Bool()) // O Trace_Signals
}

class BurstLiteWriteData(dataType: Data) extends AxiLiteWriteData(dataType) {
  val last = Output(Bool())
}

class AxiLiteWriteResponse(id_w_width: Int) extends Bundle {
  val id = Output(UInt(id_w_width.W)) // YS ID_W_WIDTH > 0
  //   val BRESP      = Input(Bool()) // O BRESP_WIDTH > 0
  //   val BUSER      = Input(Bool()) // O USER_RESP_WIDTH > 0
  //   val BTRACE     = Input(Bool()) // O Trace_Signals
  //   val BIDUNQ     = Input(Bool()) // O Unique_ID_Support
}

class AxiLiteReadRequest(addr_width: Int, id_r_width: Int) extends Bundle {
  val id   = Output(UInt(id_r_width.W)) // YS ID_R_WIDTH > 0
  val addr = Output(UInt(addr_width.W)) // Y -
  val prot = Output(UInt(3.W)) // YM PROT_Present
  //   val size     = Input(Bool()) // O SIZE_Present
  //   val user     = Input(Bool()) // O USER_REQ_WIDTH > 0
  //   val trace    = Input(Bool()) // O Trace_Signals
  //   val subsysid = Input(Bool()) // O SUBSYSID_WIDTH > 0
  //   val idunq    = Input(Bool()) // O Unique_ID_Support
}

class BurstLiteReadRequest(addr_width: Int, id_w_width: Int) extends AxiLiteReadRequest(addr_width, id_w_width) {
  val burst = Output(UInt(BurstType.getWidth.W))
  val len   = Output(UInt(8.W))
}

class AxiLiteReadData(dataType: Data, id_r_width: Int) extends Bundle {
  val id   = Output(UInt(id_r_width.W)) // YS ID_R_WIDTH > 0
  val data = Output(dataType) // Y -
  //   val resp      = Input(Bool()) // O RRESP_WIDTH > 0
  //   val user      = Input(Bool()) // O USER_DATA_WIDTH > 0 orUSER_RESP_WIDTH >
  //   val poison    = Input(Bool()) // O Poison
  //   val trace     = Input(Bool()) // O Trace_Signals
  //   val idunq     = Input(Bool()) // O Unique_ID_Support

}

class BurstLiteReadData(dataType: Data, id_r_width: Int) extends AxiLiteReadData(dataType, id_r_width) {
  val last = Output(Bool())
}

class AxiLiteIO(dataType: Data, addr_width: Int, val id_r_width: Int = 1, val id_w_width: Int = 1) extends Bundle {
  val dataWidth = dataType.getWidth

  val AW = DecoupledIO(new AxiLiteWriteRequest(addr_width, id_w_width))
  val W  = DecoupledIO(new AxiLiteWriteData(dataType))
  val B  = Flipped(DecoupledIO(new AxiLiteWriteResponse(id_w_width)))
  val AR = DecoupledIO(new AxiLiteReadRequest(addr_width, id_r_width))
  val R  = Flipped(DecoupledIO(new AxiLiteReadData(dataType, id_r_width)))
}
class BurstLiteIO(dataType: Data, addr_width: Int, id_r_width: Int = 1, id_w_width: Int = 1)
    extends AxiLiteIO(dataType, addr_width, id_r_width, id_w_width) {
  override val AW = DecoupledIO(new BurstLiteWriteRequest(addr_width, id_w_width))
  override val AR = DecoupledIO(new BurstLiteReadRequest(addr_width, id_r_width))
  override val R  = Flipped(DecoupledIO(new BurstLiteReadData(dataType, id_r_width)))
}

object AxiLiteIO {
  def apply(dataWidth: Int, addr_width:  Int) = new AxiLiteIO(UInt(dataWidth.W), addr_width)
  def apply(dataType:  Data, addr_width: Int) = new AxiLiteIO(dataType, addr_width)
}
object BurstLiteIO {
  def apply(dataWidth: Int, addr_width:  Int) = new BurstLiteIO(UInt(dataWidth.W), addr_width)
  def apply(dataType:  Data, addr_width: Int) = new BurstLiteIO(dataType, addr_width)
}

class AxiLiteArbiter(val masterPort: Int) extends Module {
  val slaveIO = IO(Vec(masterPort, Flipped(AxiLiteIO(64, 64))))
  // now just one master port
  val masterIO = IO(AxiLiteIO(64, 64))

  val workingMaster     = Reg(UInt(log2Up(masterPort).W))
  val isRead            = Reg(Bool())
  val masterRequestMask = RegInit(VecInit(Seq.fill(masterPort)(false.B)))

  val masterRequestValid = VecInit(slaveIO.map({
    case axiliteIO => axiliteIO.AR.valid || (axiliteIO.AW.valid && axiliteIO.W.valid)
  }))
  val unMaskedMasterRequestValid = VecInit(
    masterRequestValid.zip(masterRequestMask).map { case (valid, mask) => valid & !mask }
  )
  val maskedMasterRequestValid = VecInit(
    masterRequestValid.zip(masterRequestMask).map { case (valid, mask) => valid & mask }
  )
  val haveValidUnMaskedRequest = unMaskedMasterRequestValid.reduce(_ | _)
  val haveValidMaskedRequest   = maskedMasterRequestValid.reduce(_ | _)
  val haveValidRequest         = haveValidUnMaskedRequest || haveValidMaskedRequest
  val chosenUnMaskedReq        = PriorityEncoder(unMaskedMasterRequestValid)
  val chosenMaskedReq          = PriorityEncoder(maskedMasterRequestValid)

  val slaveReqFire  = VecInit(slaveIO.map(io => io.AR.fire || (io.AW.fire && io.W.fire)))
  val slaveResFire  = VecInit(slaveIO.map(io => io.R.fire || io.B.fire))
  val masterReqFire = masterIO.AR.fire || (masterIO.AW.fire && masterIO.W.fire)
  val masterResFire = masterIO.R.fire || masterIO.B.fire

  // if have Valid Masked req, choose unmasked, else masked
  val chosenReq = Mux(haveValidUnMaskedRequest, chosenUnMaskedReq, chosenMaskedReq)

  val waitMasterReq :: reqSlave :: waitSlaveRes :: resMaster :: others = Enum(4)
  val arbiterFSM = new FSM(
    waitMasterReq,
    List(
      (waitMasterReq, slaveReqFire(workingMaster), reqSlave),
      (reqSlave, masterReqFire, waitSlaveRes),
      (waitSlaveRes, masterResFire, resMaster),
      (resMaster, slaveResFire(workingMaster), waitMasterReq)
    )
  )
  val arbiterStatus = arbiterFSM.status

  val chosenMaster = Wire(Flipped(AxiLiteIO(64, 64)))
  chosenMaster := slaveIO(workingMaster)
  // unchosen ports
  slaveIO.zipWithIndex.foreach {
    case (elem, idx) =>
      elem.B.valid  := Mux(idx.U === workingMaster, chosenMaster.B.valid, false.B)
      elem.B.bits   := Mux(idx.U === workingMaster, chosenMaster.B.bits, DontCare)
      elem.R.valid  := Mux(idx.U === workingMaster, chosenMaster.R.valid, false.B)
      elem.R.bits   := Mux(idx.U === workingMaster, chosenMaster.R.bits, DontCare)
      elem.AW.ready := Mux(idx.U === workingMaster, chosenMaster.AW.ready, false.B)
      elem.AR.ready := Mux(idx.U === workingMaster, chosenMaster.AR.ready, false.B)
      elem.W.ready  := Mux(idx.U === workingMaster, chosenMaster.W.ready, false.B)
  }
  // when waitMasterReq
  workingMaster := Mux(
    arbiterFSM.is(waitMasterReq) && haveValidRequest && !arbiterFSM.willChange(),
    chosenReq,
    workingMaster
  ) // choose the chosen master
  isRead := Mux(
    arbiterFSM.is(waitMasterReq) && haveValidRequest,
    chosenMaster.AR.valid,
    isRead
  ) // check if chosen master is reading
  masterRequestMask(chosenReq) := Mux(
    arbiterFSM.is(waitMasterReq),
    true.B,
    masterRequestMask(chosenReq)
  ) // if chosen is unmasked, mask it
  chosenMaster.AR.ready := haveValidRequest && arbiterFSM.is(waitMasterReq) && isRead // change status
  chosenMaster.AW.ready := haveValidRequest && arbiterFSM.is(waitMasterReq) && !isRead
  chosenMaster.W.ready  := haveValidRequest && arbiterFSM.is(waitMasterReq) && !isRead
  val awbits = Reg(new AxiLiteWriteRequest(64, 1))
  val arbits = Reg(new AxiLiteReadRequest(64, 1))
  val wbits  = Reg(new AxiLiteWriteData(UInt(64.W)))
  awbits := Mux(slaveReqFire(workingMaster) && arbiterFSM.is(waitMasterReq), chosenMaster.AW.bits, awbits)
  wbits  := Mux(slaveReqFire(workingMaster) && arbiterFSM.is(waitMasterReq), chosenMaster.W.bits, wbits)
  arbits := Mux(slaveReqFire(workingMaster) && arbiterFSM.is(waitMasterReq), chosenMaster.AR.bits, arbits)
  // when reqSlave
  masterIO.AR.valid := arbiterFSM.is(reqSlave) && isRead
  masterIO.AR.bits  := arbits
  masterIO.AW.valid := arbiterFSM.is(reqSlave) && !isRead
  masterIO.AW.bits  := awbits
  masterIO.W.valid  := arbiterFSM.is(reqSlave) && !isRead
  masterIO.W.bits   := wbits
  // when waitSlaveRes
  val resData   = Reg(new AxiLiteReadData(UInt(64.W), 1))
  val writeBack = Reg(new AxiLiteWriteResponse(1))
  resData := Mux(
    masterResFire && arbiterFSM.is(waitSlaveRes),
    masterIO.R.bits,
    resData
  )
  writeBack := Mux(
    masterResFire && arbiterFSM.is(waitSlaveRes),
    masterIO.B.bits,
    writeBack
  )
  masterIO.B.ready := arbiterFSM.is(waitSlaveRes) && !isRead
  masterIO.R.ready := arbiterFSM.is(waitSlaveRes) && isRead

  // when resMaster
  chosenMaster.R.bits  := resData
  chosenMaster.B.bits  := writeBack
  chosenMaster.R.valid := arbiterFSM.is(resMaster) && isRead
  chosenMaster.B.valid := arbiterFSM.is(resMaster) && !isRead

}

class BurstLiteArbiter(val masterPort: Int) extends Module {
  val masterIO = IO(Vec(masterPort, Flipped(BurstLiteIO(64, 64))))
  // now just one master port
  val slaveIO = IO(BurstLiteIO(64, 64))

  val workingMaster     = Reg(UInt(log2Up(masterPort).W))
  val masterRequestMask = RegInit(VecInit(Seq.fill(masterPort)(false.B)))

  val masterReadRequestValid = VecInit(masterIO.map({
    case axiliteIO => axiliteIO.AR.valid
  }))
  val masterWriteRequestValid = VecInit(masterIO.map({
    case axiliteIO => axiliteIO.AW.valid
  }))
  val masterRequestValid =
    masterWriteRequestValid.zip(masterReadRequestValid).map { case (read, write) => read || write }
  val unMaskedMasterRequestValid = VecInit(
    masterRequestValid.zip(masterRequestMask).map { case (valid, mask) => valid & !mask }
  )
  val maskedMasterRequestValid = VecInit(
    masterRequestValid.zip(masterRequestMask).map { case (valid, mask) => valid & mask }
  )

  val haveValidUnMaskedRequest = unMaskedMasterRequestValid.reduce(_ || _)
  val haveValidMaskedRequest   = maskedMasterRequestValid.reduce(_ || _)
  val haveValidRequest         = haveValidUnMaskedRequest || haveValidMaskedRequest

  val chosenUnMaskedReq = PriorityEncoder(unMaskedMasterRequestValid)
  val chosenMaskedReq   = PriorityEncoder(maskedMasterRequestValid)
  val chosenReq         = Mux(haveValidUnMaskedRequest, chosenUnMaskedReq, chosenMaskedReq)

  val chosenIsReadReq = masterReadRequestValid(chosenReq)
  // if have Valid Masked req, choose unmasked, else masked

  val waitMasterReq :: forward :: others = Enum(4)
  val arbiterFSM = new FSM(
    waitMasterReq,
    List(
      (waitMasterReq, haveValidRequest, forward),
      (forward, (slaveIO.R.fire && slaveIO.R.bits.last) || slaveIO.B.fire, waitMasterReq)
    )
  )

  val chosenMaster = Wire(Flipped(BurstLiteIO(64, 64)))
  chosenMaster := masterIO(workingMaster)
  // unchosen ports
  masterIO.zipWithIndex.foreach {
    case (elem, idx) =>
      elem.AW.ready := Mux(idx.U === chosenReq && arbiterFSM.is(waitMasterReq), slaveIO.AW.ready, false.B)
      elem.AR.ready := Mux(idx.U === chosenReq && arbiterFSM.is(waitMasterReq), slaveIO.AR.ready, false.B)
      elem.B.valid  := Mux(idx.U === workingMaster && arbiterFSM.is(forward), slaveIO.B.valid, false.B)
      elem.B.bits   := Mux(idx.U === workingMaster, slaveIO.B.bits, DontCare)
      elem.R.valid  := Mux(idx.U === workingMaster && arbiterFSM.is(forward), slaveIO.R.valid, false.B)
      elem.R.bits   := Mux(idx.U === workingMaster, slaveIO.R.bits, DontCare)
      elem.W.ready  := Mux(idx.U === workingMaster && arbiterFSM.is(forward), slaveIO.W.ready, false.B)
  }
  // when waitMasterReq
  workingMaster := Mux(
    arbiterFSM.is(waitMasterReq) && haveValidRequest,
    chosenReq,
    workingMaster
  ) // choose the chosen master
  masterRequestMask(chosenReq) := Mux(
    arbiterFSM.is(waitMasterReq),
    true.B,
    masterRequestMask(chosenReq)
  ) // if chosen is unmasked, mask it

  slaveIO.AW.valid := arbiterFSM.is(waitMasterReq) && !chosenIsReadReq && masterIO(chosenReq).AW.valid
  slaveIO.AW.bits  := Mux(arbiterFSM.is(waitMasterReq), masterIO(chosenReq).AW.bits, DontCare)
  slaveIO.AR.valid := arbiterFSM.is(waitMasterReq) && chosenIsReadReq && masterIO(chosenReq).AR.valid
  slaveIO.AR.bits  := Mux(arbiterFSM.is(waitMasterReq), masterIO(chosenReq).AR.bits, DontCare)
  slaveIO.B.ready  := chosenMaster.B.ready && arbiterFSM.is(forward)
  slaveIO.R.ready  := chosenMaster.R.ready && arbiterFSM.is(forward)
  slaveIO.W.valid  := chosenMaster.W.valid && arbiterFSM.is(forward)
  slaveIO.W.bits   := Mux(arbiterFSM.is(forward), chosenMaster.W.bits, DontCare)
}
