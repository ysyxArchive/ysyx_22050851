#include <chrono>

#include "common.h"
#include "device.h"
#include "difftest.h"
#include "mem.h"
#include "time.h"
#include "tools/lightsss.h"

bool is_halt = false;
bool is_bad_halt = false;

uint64_t inst_count = 0;
uint64_t cycle_count = 0;

CPU cpu;
#ifdef DEBUG
LightSSS lightSSS;
#endif
int npc_clock = 0;
uint64_t* cpu_regs = NULL;
uint64_t* cpu_pc = NULL;

void haltop(unsigned char good_halt) {
  if (top->reset) return;
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
  for (int i = 0; i < 3; i++) {
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
extern "C" void mem_read(const svLogicVecVal * addr, svLogicVecVal * ret) {
  uint64_t data = read_mem(*(uint64_t*)addr, 8);
  ret[0].aval = data;
  ret[1].aval = data >> 32;
}

extern "C" void mem_write(const svLogicVecVal * addr, const svLogicVecVal * mask,
  const svLogicVecVal * data) {
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
#ifdef DEBUG
  update_cpu();
#endif
  static int lastpcchange = 0;
  static uint64_t lastpc = 0;
  if (lastpc == cpu_regs[32]) {
    lastpcchange++;
    if (lastpcchange > MAX_WAIT_ROUND) {
      Log("error pc not changed for %d cycles", MAX_WAIT_ROUND);
      is_bad_halt = true;
      is_halt = true;
    }
  }
  else {
    inst_count++;
    lastpcchange = 0;
  }
  lastpc = cpu_regs[32];
#ifdef DEBUG
  if (!difftest_check(&cpu)) {
    is_halt = true;
    is_bad_halt = true;
    return;
  }
#endif
  top->clock = 0;
  eval_trace();
#ifdef DEBUG
  if ((npc_clock / 2) % LIGHT_SSS_CYCLE_INTERVAL == 0) {
    lightSSS.do_fork();
  }
#endif
  update_device();
  cycle_count++;
}

extern uint64_t pipelineMiss[5];

void printInfo(int64_t dur) {
  Log("execute speed: %.2lf inst/s,  %ld insts, %.3f seconds, freq: %.2lf KHz",
    (double)inst_count * 1000 / dur, inst_count, (double)dur / 1000, (double)cycle_count / dur);
  Log("IPC: %.2lf inst/cycle, %ld insts, %ld cycles",
    (double)inst_count / cycle_count, inst_count, cycle_count);
  uint64_t total = 0;
  for (int i = 0; i < 5; i++) {
    total += pipelineMiss[i];
  }
  Log("if: %d(%.2f%), id: %d(%.2f%), ex: %d(%.2f%), mem: %d(%.2f%), wb: %d(%.2f%)", pipelineMiss[0], (float)pipelineMiss[0] / total * 100, pipelineMiss[1],  (float)pipelineMiss[1] / total * 100,  pipelineMiss[2],  (float)pipelineMiss[2] / total * 100, pipelineMiss[3],  (float)pipelineMiss[3] / total * 100,  pipelineMiss[4],  (float)pipelineMiss[4] / total * 100);
  printCacheRate();
}

int main(int argc, char* argv[]) {
  Log("running in " MUXDEF(DEBUG, ANSI_FMT("DEBUG", ANSI_FG_YELLOW),
    ANSI_FMT("PRODUCT", ANSI_FG_GREEN))
    ANSI_FMT(" mode", ANSI_FG_BLUE));
  parse_args(argc, argv);
  load_files();
  init_vcd_trace();
  top->reset = false;
  init_device();
#ifdef DEBUG
  lightSSS.do_fork();
#endif
  init_npc();
  update_cpu();
#ifdef DEBUG
  difftest_initial(&cpu);
#endif
  Log("init_done");

  auto start = std::chrono::high_resolution_clock::now();
  while (!is_halt) {
    one_step();
    if (cycle_count % PROFILE_LOG_INTERVAL == 0) {
      auto end = std::chrono::high_resolution_clock::now();
      auto dur =
        std::chrono::duration_cast<std::chrono::milliseconds>(end - start)
        .count();
      printInfo(dur);
    }
  }
  auto end = std::chrono::high_resolution_clock::now();
  auto dur = std::chrono::duration_cast<std::chrono::milliseconds>(end - start)
    .count();
  update_cpu();
  int ret_value = cpu.gpr[10];
  if (is_bad_halt || ret_value != 0) {
    if ((int64_t)cpu.pc - MEM_START <= 0) {
      Log(ANSI_FMT("bad halt! return value is %d, pc=0x%8lx", ANSI_FG_RED),
        ret_value, cpu.pc);
    }
    else {
      Log(ANSI_FMT("bad halt! return value is %d, pc=0x%8lx inst=0x%08x",
        ANSI_FG_RED),
        ret_value, cpu.pc, *(uint32_t*)&(mem[cpu.pc - MEM_START]));
    }
  }
  else {
    Log(ANSI_FMT("hit good trap!", ANSI_FG_GREEN));
  }
#ifdef DEBUG
  if (!lightSSS.is_child()) {
    lightSSS.wakeup_child(npc_clock);
  }
#endif
  printInfo(dur);
#ifdef DEBUG
  lightSSS.do_clear();
#endif
  return (is_bad_halt || ret_value != 0);
}