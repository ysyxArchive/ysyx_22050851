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

extern VCPU* top;
CPU cpu;
LightSSS lightSSS;
int npc_clock = 0;
uint64_t* cpu_regs = NULL;
uint64_t* cpu_pc = NULL;

void haltop(unsigned char good_halt) {
  if (top->reset)
    return;
  Log("halt from npc, is %s halt", good_halt ? "good" : "bad");
  is_halt = true;
  is_bad_halt = !good_halt;
}

void init_npc() {
#ifdef ENABLE_DEBUG
  top->enableDebug = true;
#else
  top->enableDebug = false;
#endif
  for (int i = 0; i < 10; i++) {
    top->reset = true;
    top->clock = 1;
    eval_trace();
    top->clock = 0;
    eval_trace();
  }
  top->reset = false;
}
// skip when pc is 0x00
static bool skip_once = false;
extern "C" void mem_read(const svLogicVecVal* addr, svLogicVecVal* ret) {
  uint64_t data = read_mem(*(uint64_t*)addr, 8);
  ret[0].aval = data;
  ret[1].aval = data >> 32;
}

extern "C" void mem_write(const svLogicVecVal* addr,
                          const svLogicVecVal* mask,
                          const svLogicVecVal* data) {
  uint8_t len = 0;
  auto val = mask->aval;
  while (val) {
    val >>= 1;
    len++;
  }
  uint64_t dataVal = (uint64_t)(data[1].aval) << 32 | data[0].aval;
  write_mem(*(uint64_t*)addr, len, dataVal);
}

extern "C" void set_gpr_ptr(const svOpenArrayHandle r) {
  cpu_regs = (uint64_t*)(((VerilatedDpiOpenVar*)r)->datap());
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
    return;
  }
  top->clock = 0;
  eval_trace();
  update_device();
  if ((npc_clock / 2) % LIGHT_SSS_CYCLE_INTERVAL == 0) {
    lightSSS.do_fork();
  }
}

int main(int argc, char* argv[]) {
  parse_args(argc, argv);
  load_files();
  init_vcd_trace();
  top->reset = false;
  init_device();
  lightSSS.do_fork();
  init_npc();
  update_cpu();
  difftest_initial(&cpu);
  Log("init_done");

  while (!is_halt) {
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
  lightSSS.do_clear();
  return 0;
}