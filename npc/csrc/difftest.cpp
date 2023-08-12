#include "difftest.h"
#include "mem.h"
#include <dlfcn.h>
extern const char *csrregs[];

void (*difftest_memcpy)(paddr_t addr, void *buf, size_t n,
                        bool direction) = NULL;

void (*difftest_regcpy)(void *dut, bool direction) = NULL;

void (*difftest_exec)(uint64_t n) = NULL;

void (*difftest_raise_intr)(word_t NO) = NULL;

void (*difftest_init)(int port) = NULL;

void load_difftest_so(char *diff_so_file) {
  void *handle;
  char *error;

  // 打开动态链接库
  handle = dlopen(diff_so_file, RTLD_LAZY);
  Assert(handle, "open diff so file error! %s\n", dlerror());

  // 清除之前存在的错误
  dlerror();
  // 获取函数
  *(void **)(&difftest_memcpy) = dlsym(handle, "difftest_memcpy");
  Assert(!(error = dlerror()), "load diff so symbol error! %s", error);

  *(void **)(&difftest_regcpy) = dlsym(handle, "difftest_regcpy");
  Assert(!(error = dlerror()), "load diff so symbol error! %s", error);

  *(void **)(&difftest_exec) = dlsym(handle, "difftest_exec");
  Assert(!(error = dlerror()), "load diff so symbol error! %s", error);

  *(void **)(&difftest_raise_intr) = dlsym(handle, "difftest_raise_intr");
  Assert(!(error = dlerror()), "load diff so symbol error! %s", error);

  *(void **)(&difftest_init) = dlsym(handle, "difftest_init");
  Assert(!(error = dlerror()), "load diff so symbol error! %s", error);

  Assert(difftest_memcpy && difftest_regcpy && difftest_exec &&
             difftest_raise_intr && difftest_init,
         "diff symbol check error!\n"
         "difftest_memcpy: %d\n"
         "difftest_regcpy: %d\n"
         "difftest_exec: %d\n"
         "difftest_raise_intr: %d\n"
         "difftest_init: %d\n",
         difftest_memcpy != NULL, difftest_regcpy != NULL,
         difftest_exec != NULL, difftest_raise_intr != NULL,
         difftest_init != NULL);
}

bool skip_once = false;
void difftest_skip() { skip_once = true; }

void difftest_step(CPU *cpu) {
  if (skip_once == true) {
    difftest_regcpy(cpu, TO_REF);
    skip_once = false;
  } else {
    difftest_exec(1);
  }
}

void difftest_check(CPU *cpu) {
  CPU refcpu;
  difftest_step(cpu);
  difftest_regcpy(&refcpu, FROM_REF);
  bool difftest_failed = false;
  if (cpu->pc != refcpu.pc) {
    printf("Difftest Failed\n Expected pc: %llx, Actual pc: %llx \n", refcpu.pc,
           cpu->pc);
    difftest_failed = true;
  }
  for (int i = 0; i < 32; i++) {
    if (cpu->gpr[i] != refcpu.gpr[i]) {
      printf("Difftest Failed\ncheck reg[%d] failed before pc:%llx\nExpected: "
             "%llx, Actual: %llx \n",
             i, cpu->pc, refcpu.gpr[i], cpu->gpr[i]);
      difftest_failed = true;
    }
  }
  for (int i = 0; i < 6; i++) {
    if (cpu->csr[i] != refcpu.csr[i]) {
      printf("Difftest Failed\ncheck csr %s failed before pc:%llx\nExpected: "
             "%llx, Actual: %llx \n",
             csrregs[i], cpu->pc, refcpu.gpr[i], cpu->gpr[i]);
      difftest_failed = true;
    }
  }

  if (difftest_failed) {
    isa_reg_display();
    assert(0);
  }
  // TODO: difftest_checkmem
  // difftest_checkmem(cpu);
  return;
}

void difftest_checkmem(CPU *cpu) {
  uint64_t from_ref = 0, local = 0;
  for (uint64_t addr = MEM_START; addr <= MEM_START + MEM_LEN - 0x8;
       addr += 0x8) {
    difftest_memcpy(addr, &from_ref, 8, FROM_REF);
    local = read_mem_nolog(addr, 0x8);
    Assert(from_ref == local,
           "mem check error at %016lx when pc is %llx! \n expected: %016lx "
           "actual: %016lx",
           addr, cpu->pc, from_ref, local);
  }
}

void difftest_initial(CPU *cpu) {
  Log("difftest_init");
  difftest_regcpy(cpu, TO_REF);
  Log("difftest_memcpy, %d", difftest_memcpy);
  difftest_memcpy(MEM_START, mem, MEM_LEN, TO_REF);
  Log("difftest_init done");
}