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
  if (top->reset)
    return;
  Log("halt from npc, is %s halt", good_halt ? "good" : "bad");
  is_halt = true;
  is_bad_halt = !good_halt;
}

extern VCPU *top;
CPU cpu;
LightSSS lightSSS;
int npc_clock = 0;
uint64_t *cpu_regs = NULL;
uint64_t *cpu_pc = NULL;

void init_npc() {
  for (int i = 0; i < 10; i++) {
    top->reset = true;
    top->clock = 1;
    top->eval();
    top->clock = 0;
    top->eval();
  }
  top->reset = false;
}

extern "C" void mem_read(const svLogicVecVal *addr, const svLogicVecVal *len,
                         svLogicVecVal *ret, unsigned char is_unsigned) {
  uint64_t data = read_mem(*(uint64_t *)addr, *(uint8_t *)len);
  if (!is_unsigned) {
    if (*(uint8_t *)len == 1) {
      data = (uint64_t)(int64_t)(int8_t)data;
    } else if (*(uint8_t *)len == 2) {
      data = (uint64_t)(int64_t)(int16_t)data;
    } else if (*(uint8_t *)len == 4) {
      data = (uint64_t)(int64_t)(int32_t)data;
    } else if (*(uint8_t *)len == 8) {
      data = (uint64_t)(int64_t)(int64_t)data;
    }
  }
  ret[0].aval = data;
  ret[1].aval = data >> 32;
}

extern "C" void mem_write(const svLogicVecVal *addr, const svLogicVecVal *len,
                          const svLogicVecVal *data) {
  uint64_t dataVal = (uint64_t)(data[1].aval) << 32 | data[0].aval;
  write_mem(*(uint64_t*)addr, len, dataVal);
}

extern "C" void set_gpr_ptr(const svOpenArrayHandle r) {
  cpu_regs = (uint64_t *)(((VerilatedDpiOpenVar *)r)->datap());
}

void update_cpu() {
  memcpy(&(cpu.gpr), cpu_gpr, 32 * sizeof(uint64_t));
  cpu.pc = cpu_gpr[32];
  Log("updating cpu , pc is %lx", cpu.pc);
}
void one_step() {
  // 记录波形
  top->clock = 1;
  eval_trace();
  uint64_t npc = top->pcio_pc;
  top->pcio_inst = read_mem_nolog(npc, 4);
  update_cpu();

  static int latpcchange = 0;
  static uint64_t lastpc = 0;
  if (lastpc == cpu.pc) {
    latpcchange++;
    if (latpcchange > MAX_WAIT_ROUND) {
      Log("error pc not changed for %d cycles", MAX_WAIT_ROUND);
      is_bad_halt = true;
      is_halt = true;
    }
  } else {
    latpcchange = 0;
  }
  lastpc = cpu.pc;

  if (!difftest_check(&cpu)) {
    is_halt = true;
    is_bad_halt = true;
  }
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
  init_device();
  init_npc();
  update_cpu();
  difftest_initial(&cpu);
  lightSSS.do_fork();
  Log("init_done");

  tfp->dump(npc_clock++);
  while (!is_halt && npc_clock < 50) {
    one_step();
  }
  int ret_value = cpu.gpr[10];
  if (is_bad_halt || ret_value != 0) {
    Log("bad halt! pc=0x%lx inst=0x%08x", cpu.pc,
        *(uint32_t*)&(mem[cpu.pc - MEM_START]));
    if (!lightSSS.is_child()) {
      lightSSS.wakeup_child(npc_clock);
    }
    exit(-1);
  }
  Log(ANSI_FMT("hit good trap!", ANSI_FG_GREEN));
  return 0;
}