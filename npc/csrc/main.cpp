#include "VCPU.h"
#include "VCPU__Dpi.h"
#include "mem.h"
#include "stdio.h"
#include "verilated.h"
#include "verilated_dpi.h"
#include "verilated_vcd_c.h"

bool is_halt = false;
bool is_bad_halt = false;
void haltop(unsigned char good_halt) {
  is_halt = true;
  is_bad_halt = !good_halt;
}

uint64_t* cpu_gpr = NULL;
uint64_t cpu_pc = 0;
extern "C" void set_gpr_ptr(const svOpenArrayHandle r) {
  cpu_gpr = (uint64_t*)(((VerilatedDpiOpenVar*)r)->datap());
}

// 一个输出RTL中通用寄存器的值的示例
void dump_gpr() {
  int i;
  for (i = 0; i < 32; i++) {
    printf("gpr[%d] = 0x%lx\n", i, cpu_gpr[i]);
  }
}

extern "C" void set_pc(const svLogicVecVal* pc) {
  cpu_pc =  pc->bval << 32 | pc->aval;
  printf("pc is 0x%lx\n", pc->aval);
}

int main(int argc, char** argv) {
  printf("%d %s\n", argc, argv[1]);
  init_memory(argv[1]);
  Verilated::commandArgs(argc, argv);
  Verilated::traceEverOn(true);  // 导出vcd波形需要加此语句
  VerilatedContext* contextp = new VerilatedContext;
  contextp->commandArgs(argc, argv);

  Verilated::traceEverOn(true);  // 导出vcd波形需要加此语句
  VerilatedVcdC* tfp = new VerilatedVcdC();  // 导出vcd波形需要加此语句

  VCPU* top = new VCPU{contextp};
  top->trace(tfp, 0);
  tfp->open("wave.vcd");        // 打开vcd
  top->pcio_inst = 0x00000013;  // 默认为 addi e0, 0;
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
  //   while (time < 100 && top->pcio_pc != 0) {
  while (!is_halt) {
    uint64_t pc = top->pcio_npc;
    dump_gpr();

    top->pcio_inst = read_mem(pc, 4);
    top->eval();
    // 记录波形
    top->clock = 1;
    top->eval();
    tfp->dump(time++);
    top->clock = 0;
    top->eval();
    tfp->dump(time++);
    // 推动
    tfp->flush();
  }
  delete top;
  delete contextp;
  delete tfp;
  Assert(!is_bad_halt, "bad halt! \npc=0x%lx inst=0x%08x", top->pcio_npc - 4,
         top->pcio_inst);
  printf("hit good trap!\n");
  return 0;
}