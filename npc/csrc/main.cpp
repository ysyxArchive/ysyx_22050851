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
  is_halt = true;
  is_bad_halt = !good_halt;
}

extern VCPU *top;
CPU cpu;
LightSSS lightSSS;
int npc_clock = 0;
uint64_t *cpu_gpr = NULL;
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
  cpu_gpr = (uint64_t *)(((VerilatedDpiOpenVar *)r)->datap());
}

void update_cpu() {
  memcpy(&(cpu.gpr), cpu_gpr, 32 * sizeof(uint64_t));
  cpu.pc = cpu_gpr[32];
  // TODO: ITRACE
  //  Log("updating cpu , pc is %lx", cpu.pc);
}

void one_step() {
  // 记录波形
  top->clock = 1;
  eval_trace();
  uint64_t npc = top->pcio_pc;
  top->pcio_inst = read_mem_nolog(npc, 4);
  update_cpu();
  difftest_check(&cpu);
  top->clock = 0;
  eval_trace();
  if ((npc_clock / 2) % LIGHT_SSS_CYCLE_INTERVAL == 0) {
    lightSSS.do_fork();
  }
  if (npc_clock > 12322333) {
    is_halt = true;
    is_bad_halt = true;
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

  Log("init_done");

  while (!is_halt) {
    one_step();
  }

  if (is_bad_halt) {
    Log("bad halt! \npc=0x%lx inst=0x%08x", top->pcio_pc, top->pcio_inst);
    if (!lightSSS.is_child()) {
      lightSSS.wakeup_child(npc_clock);
    }
    Log("exit");
    exit(-1);
  }
  Log(ANSI_FMT("hit good trap!", ANSI_FG_GREEN));
  lightSSS.do_clear();
  return 0;
}