#include "VCPU.h"
#include "VCPU__Dpi.h"
#include "common.h"
#include "device.h"
#include "difftest.h"
#include "mem.h"
#include "tools/lightsss.h"
#include "verilated.h"
#include "verilated_dpi.h"
#include "verilated_vcd_c.h"

bool is_halt = false;
bool is_bad_halt = false;

void haltop(unsigned char good_halt) {
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
    eval_trace();
    top->clock = 0;
    eval_trace();
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
  write_mem(*(uint64_t *)addr, *(uint8_t *)len, dataVal);
}

extern "C" void set_gpr_ptr(const svOpenArrayHandle r) {
  cpu_regs = (uint64_t *)(((VerilatedDpiOpenVar *)r)->datap());
}

void update_cpu() {
  memcpy(&(cpu.gpr), cpu_regs, 32 * sizeof(uint64_t));
  cpu.pc = cpu_regs[32];
  memcpy(&(cpu.csr), cpu_regs + 32 + 1, 6 * sizeof(uint64_t));
  // TODO: ITRACE
  //  Log("updating cpu , pc is %lx", cpu.pc);
}

void one_step() {
  // 记录波形
  top->clock = 1;
  eval_trace();
  update_cpu();
  if (!difftest_check(&cpu)) {
    is_halt = true;
    is_bad_halt = true;
  }
  top->clock = 0;
  eval_trace();
  update_device();
  if ((npc_clock / 2) % LIGHT_SSS_CYCLE_INTERVAL == 0) {
    lightSSS.do_fork();
  }
}

int main(int argc, char *argv[]) {
  parse_args(argc, argv);
  load_files();
  init_vcd_trace();
  top->reset = false;
  init_device();
  init_npc();
  update_cpu();
  difftest_initial(&cpu);
  lightSSS.do_fork();
  Log("init_done");

  while (!is_halt) {
    one_step();
  }
  int ret_value = cpu.gpr[10];
  if (is_bad_halt || ret_value) {
    Log("bad halt! pc=0x%lx inst=0x%08x", cpu.pc, *(uint32_t *)&(mem[cpu.pc]));
    if (!lightSSS.is_child()) {
      lightSSS.wakeup_child(npc_clock);
    }
    exit(-1);
  }
  Log(ANSI_FMT("hit good trap!", ANSI_FG_GREEN));
  lightSSS.do_clear();
  return 0;
}