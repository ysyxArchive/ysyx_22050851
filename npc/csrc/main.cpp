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
<<<<<<< HEAD
=======

extern VCPU* top;
CPU cpu;
LightSSS lightSSS;
int npc_clock = 0;
uint64_t* cpu_regs = NULL;
uint64_t* cpu_pc = NULL;

>>>>>>> npc
void haltop(unsigned char good_halt) {
  if (top->reset)
    return;
  Log("halt from npc, is %s halt", good_halt ? "good" : "bad");
  is_halt = true;
  is_bad_halt = !good_halt;
}

<<<<<<< HEAD
VCPU* top;
VerilatedVcdC* tfp;
CPU cpu;

uint64_t* cpu_gpr = NULL;
uint64_t* cpu_pc = NULL;
int npc_clock = 0;

void init_npc() {
  top->trace(tfp, 0);
  tfp->open("wave.vcd");        // 打开vcd
  top->pcio_inst = 0x00000013;  // 默认为 addi e0, 0;
=======
void init_npc() {
#ifdef ENABLE_DEBUG
  top->enableDebug = true;
#else
  top->enableDebug = false;
#endif
>>>>>>> npc
  for (int i = 0; i < 10; i++) {
    top->reset = true;
    top->clock = 1;
    top->eval();
    top->clock = 0;
    top->eval();
  }
  top->reset = false;
}
<<<<<<< HEAD

extern "C" void mem_read(const svLogicVecVal* addr,
                         const svLogicVecVal* len,
                         svLogicVecVal* ret) {
  uint64_t data = read_mem(*(uint64_t*)addr, *(uint8_t*)len);
=======
// skip when pc is 0x00
static bool skip_once = false;
extern "C" void mem_read(const svLogicVecVal* addr, svLogicVecVal* ret) {
  uint64_t data = read_mem(*(uint64_t*)addr, 8);
>>>>>>> npc
  ret[0].aval = data;
  ret[1].aval = data >> 32;
}

extern "C" void mem_write(const svLogicVecVal* addr,
<<<<<<< HEAD
                          const svLogicVecVal* len,
                          const svLogicVecVal* data) {
  uint64_t dataVal = (uint64_t)(data[1].aval) << 32 | data[0].aval;
  write_mem(*(uint64_t*)addr, *(uint8_t*)len, dataVal);
}

extern "C" void set_gpr_ptr(const svOpenArrayHandle r) {
  cpu_gpr = (uint64_t*)(((VerilatedDpiOpenVar*)r)->datap());
=======
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
>>>>>>> npc
}

void update_cpu() {
  memcpy(&(cpu.gpr), cpu_gpr, 32 * sizeof(uint64_t));
  cpu.pc = cpu_gpr[32];
  Log("updating cpu , pc is %lx", cpu.pc);
}
void one_step() {
  // 记录波形
  top->clock = 1;
<<<<<<< HEAD
  top->eval();
  tfp->dump(npc_clock++);
  uint64_t npc = top->pcio_pc;
  top->pcio_inst = read_mem_nolog(npc, 4);
  tfp->flush();
  update_cpu();
  difftest_check(&cpu);
=======
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
  }
>>>>>>> npc
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
<<<<<<< HEAD
  init_npc();
  update_cpu();
  difftest_initial(&cpu);

=======
  init_device();
  lightSSS.do_fork();
  init_npc();
  update_cpu();
  difftest_initial(&cpu);
>>>>>>> npc
  Log("init_done");

  tfp->dump(npc_clock++);
  while (!is_halt && npc_clock < 50) {
    one_step();
  }
<<<<<<< HEAD

  delete top;
  delete contextp;
  delete tfp;

  Assert(!is_bad_halt, "bad halt! \npc=0x%lx inst=0x%08x", top->pcio_pc,
         top->pcio_inst);
=======
  int ret_value = cpu.gpr[10];
  if (is_bad_halt || ret_value != 0) {
    Log("bad halt! pc=0x%lx inst=0x%08x", cpu.pc,
        *(uint32_t*)&(mem[cpu.pc - MEM_START]));
    if (!lightSSS.is_child()) {
      lightSSS.wakeup_child(npc_clock);
    }
    exit(-1);
  }
>>>>>>> npc
  Log(ANSI_FMT("hit good trap!", ANSI_FG_GREEN));
  return 0;
}