#include "VCPU.h"
#include "VCPU__Dpi.h"
#include "common.h"
#include "difftest.h"
#include "mem.h"
#include "verilated.h"
#include "verilated_dpi.h"
#include "verilated_vcd_c.h"

bool is_halt = false;
bool is_bad_halt = false;
void haltop(unsigned char good_halt) {
  is_halt = true;
  is_bad_halt = !good_halt;
}

VCPU* top;
VerilatedVcdC* tfp;
CPU cpu;

void init_npc() {
  top->trace(tfp, 0);
  tfp->open("wave.vcd");        // 打开vcd
  top->pcio_inst = 0x00000013;  // 默认为 addi e0, 0;
  for (int i = 0; i < 10; i++) {
    top->clock = 1;
    top->eval();
    top->clock = 0;
    top->eval();
    top->reset = true;
    top->eval();
  }
  top->reset = false;
}

uint64_t* cpu_gpr = NULL;
uint64_t* cpu_pc = NULL;
int npc_clock = 0;

extern "C" void mem_read(const svLogicVecVal* addr,
                         const svLogicVecVal* len,
                         svLogicVecVal* ret) {
  *(uint64_t*)ret = read_mem(*(uint64_t*)addr, *(uint8_t*)len);
}

extern "C" void mem_write(const svLogicVecVal* addr,
                          const svLogicVecVal* len,
                          const svLogicVecVal* data) {
  write_mem(*(uint64_t*)addr, *(uint8_t*)len, *(uint64_t*)data);
}

extern "C" void set_gpr_ptr(const svOpenArrayHandle r) {
  cpu_gpr = (uint64_t*)(((VerilatedDpiOpenVar*)r)->datap());
}

void update_cpu() {
  memcpy(&(cpu.gpr), cpu_gpr, 32 * sizeof(uint64_t));
  cpu.pc = cpu_gpr[32];
  Log("updating cpu , pc is %lx, %lx", cpu.pc, 0);
}

void one_step() {
  // 记录波形
  top->clock = 1;
  top->eval();
  tfp->dump(npc_clock++);
  uint64_t npc = top->pcio_pc;
  top->pcio_inst = read_mem(npc, 4);
  update_cpu();
  tfp->flush();
  difftest_check(&cpu);
  top->clock = 0;
  top->eval();
  tfp->dump(npc_clock++);
  // 推动
  tfp->flush();
}

int main(int argc, char* argv[]) {
  parse_args(argc, argv);
  load_files();
  // TODO: 传参不对
  // Verilated::commandArgs(argc, argv);
  VerilatedContext* contextp = new VerilatedContext;
  // TODO: 传参不对
  // contextp->commandArgs(argc, argv);
  Verilated::traceEverOn(true);  // 导出vcd波形需要加此语句
  tfp = new VerilatedVcdC();     // 导出vcd波形需要加此语句
  top = new VCPU{contextp};
  top->reset = false;
  init_npc();
  update_cpu();
  difftest_initial(&cpu);

  Log("init_done");

  tfp->dump(npc_clock++);
  while (!is_halt && npc_clock < 50) {
    one_step();
  }

  delete top;
  delete contextp;
  delete tfp;

  Assert(!is_bad_halt, "bad halt! \npc=0x%lx inst=0x%08x", top->pcio_pc,
         top->pcio_inst);
  Log(ANSI_FMT("hit good trap!", ANSI_FG_GREEN));
  return 0;
}