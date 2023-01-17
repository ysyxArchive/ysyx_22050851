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

uint32_t mem[] = {
    0x108113,  // 0000000 00001 00000 000 00010 00100 11 : reg 2 = reg0(0) +  1
    0x110093,  // 0000000 00001 00010 000 00001 00100 11 : reg 1 = reg2 +  1
    0x110093,  // 0000000 00001 00010 000 00001 00100 11 : reg 1 = reg2 +  1
    0x110093,  // 0000000 00001 00010 000 00001 00100 11 : reg 1 = reg2 +  1
    0x110093,  // 0000000 00001 00010 000 00001 00100 11 : reg 1 = reg2 +  1
    0x110093,  // 0000000 00001 00010 000 00001 00100 11 : reg 1 = reg2 +  1
};
int main(int argc, char** argv) {
  Verilated::commandArgs(argc, argv);
  Verilated::traceEverOn(true);  // 导出vcd波形需要加此语句
  VerilatedContext* contextp = new VerilatedContext;
  contextp->commandArgs(argc, argv);

  Verilated::traceEverOn(true);  // 导出vcd波形需要加此语句
  VerilatedVcdC* tfp = new VerilatedVcdC();  // 导出vcd波形需要加此语句

  VCPU* top = new VCPU{contextp};
  top->trace(tfp, 0);
  tfp->open("wave.vcd");  // 打开vcd
  int time = 0;
  for (int i = 0; i < 10; i++) {
    top->reset = true;
    top->eval();
  }
  top->reset = false;

  while (time < 100 && top->pcio_pc <= 0x80000000 + 6 * 4) {
    printf("now the pc is %lx\n", top->pcio_pc);
    // top->pcio_inst = mem[(top->pcio_pc - 0x80000000) / 4];
    // uint64_t pc = top->pcio_pc;

    // 记录波形
    top->clock = 0;
    top->eval();
    tfp->dump(time++);
    top->clock = 1;
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