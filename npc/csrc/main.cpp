#include "VCPU.h"
#include "stdio.h"
#include "verilated.h"
#include "verilated_vcd_c.h"

uint32_t mem[] = {
    0x100113,  // 0000000 00001 00000 000 00010 00100 11 : reg 2 = reg0(0) +  1
    0x110113,  // 0000000 00001 00010 000 00010 00100 11 : reg 2 = reg2 +  1
    0x110113,  // 0000000 00001 00010 000 00010 00100 11 : reg 2 = reg2 +  1
    0x110113,  // 0000000 00001 00010 000 00010 00100 11 : reg 2 = reg2 +  1
    0x110113,  // 0000000 00001 00010 000 00010 00100 11 : reg 2 = reg2 +  1
    0x110113,  // 0000000 00001 00010 000 00010 00100 11 : reg 2 = reg2 +  1
    0x110113,  // 0000000 00001 00010 000 00010 00100 11 : reg 2 = reg2 +  1
    0x110113,  // 0000000 00001 00010 000 00010 00100 11 : reg 2 = reg2 +  1
    0x110113,  // 0000000 00001 00010 000 00010 00100 11 : reg 2 = reg2 +  1
    0x110113,  // 0000000 00001 00010 000 00010 00100 11 : reg 2 = reg2 +  1
    0x110113,  // 0000000 00001 00010 000 00010 00100 11 : reg 2 = reg2 +  1
    0x110113,  // 0000000 00001 00010 000 00010 00100 11 : reg 2 = reg2 +  1
    0x110113,  // 0000000 00001 00010 000 00010 00100 11 : reg 2 = reg2 +  1
    0x110113,  // 0000000 00001 00010 000 00010 00100 11 : reg 2 = reg2 +  1
    0x110113,  // 0000000 00001 00010 000 00010 00100 11 : reg 2 = reg2 +  1
    0x110113,  // 0000000 00001 00010 000 00010 00100 11 : reg 2 = reg2 +  1
    0x100073  // 0000000 00001 00000 000 00000 11100 11 : halt

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
    top->clock = 1;
    top->eval();
    top->clock = 0;
    top->eval();
    top->reset = true;
    top->eval();
  }
  top->reset = false;

  tfp->dump(time++);
  while (time < 100 && top->pcio_pc != 0) {
    uint64_t pc = top->pcio_pc;
    printf("now the pc is %lx %d\n", top->pcio_pc, (pc - 0x80000000) / 4);

    top->pcio_inst = mem[(pc - 0x80000000) / 4];
    top->eval();
    // 记录波形
    top->clock = 1;
    top->eval();
    tfp->dump(time++);
    top->clock = 0;
    top->eval();
    tfp->dump(time++);
    // 推动
  }
  delete top;
  delete contextp;
  delete tfp;
  assert(top->pcio_pc == 0);
  printf("hit good trap!\n");
  return 0;
}