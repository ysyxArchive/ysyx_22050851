#include "VCPU.h"
#include "stdio.h"
#include "verilated.h"
#include "verilated_vcd_c.h"

static void single_cycle(VCPU* top) {
  top->clock = 0;
  top->eval();
  top->clock = 1;
  top->eval();
}

int main(int argc, char** argv) {
  VerilatedContext* contextp = new VerilatedContext;
  contextp->commandArgs(argc, argv);

  Verilated::traceEverOn(true);  // 导出vcd波形需要加此语句
  VerilatedVcdC* tfp = new VerilatedVcdC();  // 导出vcd波形需要加此语句

  VCPU* top = new VCPU{contextp};
  top->trace(tfp, 0);
  tfp->open("wave.vcd");  // 打开vcd
  int time = 0;
  while (time < 20) {
    top->pcio_inst = 0;
    uint64_t pc = top->pcio_pc;
    printf("%lu %lu\n", top->pcio_inst, pc);

    // 记录波形
    top->eval();
    tfp->dump(time++);
    // 推动
    single_cycle(top);
  }
  delete top;
  delete contextp;
  delete tfp;
  return 0;
}