#include "difftest.h"
#include <dlfcn.h>
#include "common.h"

void (*difftest_memcpy)(paddr_t addr,
                        void* buf,
                        size_t n,
                        bool direction) = NULL;

void (*difftest_regcpy)(void* dut, bool direction) = NULL;

void (*difftest_exec)(uint64_t n) = NULL;

void (*difftest_raise_intr)(word_t NO) = NULL;

void (*difftest_init)(int port) = NULL;

void load_difftest_so(char* diff_so_file) {
  void* handle;
  char* error;

  // 打开动态链接库
  handle = dlopen(diff_so_file, RTLD_LAZY);
  Assert(handle, "open diff so file error! %s\n", dlerror());

  // 清除之前存在的错误
  dlerror();
  // 获取函数
  *(void**)(&difftest_memcpy) = dlsym(handle, "difftest_memcpy");
  Assert(!(error = dlerror()), "load diff so symbol error! %s", error);

  *(void**)(&difftest_regcpy) = dlsym(handle, "difftest_regcpy");
  Assert(!(error = dlerror()), "load diff so symbol error! %s", error);

  *(void**)(&difftest_exec) = dlsym(handle, "difftest_exec");
  Assert(!(error = dlerror()), "load diff so symbol error! %s", error);

  *(void**)(&difftest_raise_intr) = dlsym(handle, "difftest_raise_intr");
  Assert(!(error = dlerror()), "load diff so symbol error! %s", error);

  *(void**)(&difftest_init) = dlsym(handle, "difftest_init");
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
CPU lastcpu = {.pc = 0};
void difftest_check(CPU* cpu) {
  if (lastcpu.pc != 0 && lastcpu.pc != cpu->pc) {
    CPU refcpu;
    difftest_regcpy(&refcpu, FROM_REF);
    Assert(lastcpu.pc == refcpu.pc,
           "Difftest Failed\n Expected pc: %llx, Actual pc: %llx ", refcpu.pc,
           lastcpu.pc);
    for (int i = 0; i < 32; i++) {
      Assert(lastcpu.gpr[i] == refcpu.gpr[i],
             "Difftest Failed\ncheck reg[%d] failed when pc:%llx\nExpected: "
             "%llx, Actual: %llx ",
             i, lastcpu.pc, refcpu.gpr[i], lastcpu.gpr[i]);
    }
    difftest_exec(1);
    memcpy(&lastcpu, cpu, sizeof(CPU));
  }
  return;
}

void difftest_initial(CPU* cpu) {
  difftest_regcpy(cpu, TO_REF);
  difftest_memcpy(MEM_START, mem, bin_file_size, TO_REF);
}