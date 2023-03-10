import chisel3.experimental._
import chisel3._
import chisel3.util._
import decode._
import os.read
import execute._
import utils._
import firrtl.seqCat

class InstructionExecuteUnit extends Module {
  val in       = IO(Flipped(Decoupled(Vec(2, Operation()))))
  val regIO    = IO(Flipped(new RegisterFileIO()))
  val memIO    = IO(Flipped(new MemIO()))
  val blackBox = Module(new BlackBoxHalt);
  val alu      = Module(new ALU)
  val pcAdder  = Module(new SimpleAdder)

  val regReadPort0ID = Wire(UInt(4.W))
  val regReadPort1ID = Wire(UInt(4.W))

  val ops = in.bits;
  val op0 = ops(0);
  val op1 = ops(1);

  val temp = Wire(UInt(64.W))

  in.ready := true.B

  val op0src1val =
    MuxLookup(
      op0.src1.stype,
      0.U,
      Seq(
        (SourceType.imm.asUInt) -> op0.src1.value,
        (SourceType.reg.asUInt) -> Mux(regReadPort0ID === 0.U, regIO.out0, regIO.out1),
        (SourceType.regLow.asUInt) -> Utils
          .signalExtend(Mux(regReadPort0ID === 0.U, regIO.out0, regIO.out1)(31, 0), 32),
        (SourceType.npc.asUInt) -> regIO.npc,
        (SourceType.pc.asUInt) -> regIO.pc
      )
    );
  val op0src2val =
    MuxLookup(
      op0.src2.stype,
      0.U,
      Seq(
        (SourceType.imm.asUInt) -> op0.src2.value,
        (SourceType.reg.asUInt) -> Mux(regReadPort0ID === 1.U, regIO.out0, regIO.out1),
        (SourceType.regLow.asUInt) -> Utils
          .signalExtend(Mux(regReadPort0ID === 1.U, regIO.out0, regIO.out1)(31, 0), 32),
        (SourceType.npc.asUInt) -> regIO.npc,
        (SourceType.pc.asUInt) -> regIO.pc
      )
    );
  val op1src1val =
    MuxLookup(
      op1.src1.stype,
      0.U,
      Seq(
        (SourceType.imm.asUInt) -> op1.src1.value,
        (SourceType.reg.asUInt) -> Mux(regReadPort0ID === 2.U, regIO.out0, regIO.out1),
        (SourceType.regLow.asUInt) -> Utils
          .signalExtend(Mux(regReadPort0ID === 2.U, regIO.out0, regIO.out1)(31, 0), 32),
        (SourceType.npc.asUInt) -> regIO.npc,
        (SourceType.pc.asUInt) -> regIO.pc,
        (SourceType.alu.asUInt) -> alu.io.out,
        (SourceType.temp.asUInt) -> temp
      )
    );
  val op1src2val =
    MuxLookup(
      op1.src2.stype,
      0.U,
      Seq(
        (SourceType.imm.asUInt) -> op1.src2.value,
        (SourceType.reg.asUInt) -> Mux(regReadPort0ID === 3.U, regIO.out0, regIO.out1),
        (SourceType.regLow.asUInt) -> Utils
          .signalExtend(Mux(regReadPort0ID === 3.U, regIO.out0, regIO.out1)(31, 0), 32),
        (SourceType.npc.asUInt) -> regIO.npc,
        (SourceType.pc.asUInt) -> regIO.pc,
        (SourceType.alu.asUInt) -> alu.io.out,
        (SourceType.temp.asUInt) -> temp
      )
    );

  val ans0 = MuxLookup(
    op0.opType.asUInt,
    0.U,
    Seq(
      OperationType.add.asUInt -> alu.io.out,
      OperationType.sub.asUInt -> alu.io.out,
      OperationType.move.asUInt -> op0src1val
    )
  )
  val ans1 = MuxLookup(
    op1.opType.asUInt,
    0.U,
    Seq(
      OperationType.move.asUInt -> op1src1val,
      OperationType.loadmemU.asUInt -> memIO.rdata,
      OperationType.loadmemS.asUInt -> Utils.signalExtend(memIO.rdata, op1.dst.value),
      OperationType.savemem.asUInt -> op1src2val,
      OperationType.moveBranch.asUInt -> op1src1val
    )
  )
  // blackbox config
  blackBox.io.halt     := op0.opType === OperationType.halt.asUInt
  blackBox.io.bad_halt := op0.opType === OperationType.noMatch.asUInt;
  // tempWire config
  temp := ans0
  // memory config
  val memTargetIndex =
    op1.opType === OperationType.loadmemS.asUInt ||
      op1.opType === OperationType.loadmemU.asUInt ||
      op1.opType === OperationType.savemem.asUInt
  val memTargetOp = Mux(memTargetIndex, op1, op0)
  memIO.addr := Mux(memTargetIndex, op1src1val, op0src1val)
  memIO.len  := Mux(memTargetIndex, op1.dst.value, op0.dst.value)
  memIO.enable :=
    memTargetOp.opType === OperationType.loadmemS.asUInt ||
      memTargetOp.opType === OperationType.loadmemU.asUInt ||
      memTargetOp.opType === OperationType.savemem.asUInt
  memIO.isRead := memTargetOp.opType =/= OperationType.savemem.asUInt
  memIO.wdata  := Mux(memTargetIndex, ans1, ans0)
  // register config
  val srcPorts   = Seq(op0.src1, op0.src2, op1.src1, op1.src2)
  val regValidOH = VecInit(srcPorts.map(src => Utils.isRegType(src))).asUInt;
  regReadPort0ID := PriorityEncoder(regValidOH)
  regReadPort1ID := PriorityEncoder(Reverse(regValidOH))
  regIO.raddr0   := PriorityMux(regValidOH, srcPorts).value
  regIO.raddr1   := PriorityMux(Reverse(regValidOH), srcPorts.reverse).value

  regIO.wdata := Mux(
    Utils.isRegType(op0.dst),
    Mux(op0.dst.stype === SourceType.regLow.asUInt, Utils.signalExtend(ans0(31, 0), 32), ans0),
    Mux(op1.dst.stype === SourceType.regLow.asUInt, Utils.signalExtend(ans1(31, 0), 32), ans1)
  )
  regIO.waddr := PriorityMux(
    Seq(
      Utils.isRegType(op0.dst) -> op0.dst.value,
      Utils.isRegType(op1.dst) -> op1.dst.value,
      true.B -> 0.U
    )
  )

  MuxLookup(
    SourceType.reg.asUInt,
    0.U,
    Seq(
      op0.dst.stype -> op0.dst.value,
      op1.dst.stype -> op1.dst.value
    )
  )
  regIO.dnpc := MuxLookup(
    OperationType.moveBranch.asUInt,
    Mux(op0.dst.stype === SourceType.pc.asUInt, ans0, ans1),
    Seq(
      op0.opType -> pcAdder.io.out,
      op1.opType -> pcAdder.io.out
    )
  )
  regIO.pcWrite := MuxLookup(
    SourceType.pc.asUInt,
    false.B,
    Seq(
      (op0.dst.stype) ->
        ((op0.opType === OperationType.moveBranch.asUInt) ^ !ALUUtils.test(
          alu.io.signals,
          op0.dst.value
        )),
      (op1.dst.stype) ->
        (op1.opType === (OperationType.moveBranch.asUInt) ^ ALUUtils.test(
          alu.io.signals,
          op1.dst.value
        ))
    )
  )
  // alu config
  alu.io.inA := op0src1val;
  alu.io.inB := op0src2val;
  alu.io.opType := MuxLookup(
    op0.opType.asUInt,
    ALUType.add,
    Seq(
      OperationType.add.asUInt -> ALUType.add,
      OperationType.sub.asUInt -> ALUType.sub
    )
  )
  // pcAdder config
  pcAdder.io.inA := regIO.pc
  pcAdder.io.inB := Mux(op0.opType === OperationType.moveBranch.asUInt, op0, op1).src1.value
  pcAdder.io.inC := 0.U
}
