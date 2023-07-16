#include "fs.h"
#include <loader.h>
#include <proc.h>
#include <string.h>
#define MAX_NR_PROC 4

static PCB pcb[MAX_NR_PROC] __attribute__((used)) = {};
static PCB pcb_boot = {};
PCB *current = NULL;
int pcbcount = 0;
void switch_boot_pcb() { current = &pcb_boot; }
PCB *executing[2];
void hello_fun(void *arg) {
  int j = 1;
  while (1) {
    if (j % 200 == 0)
      Log("Hello World from Nanos-lite with arg '%s' for the %dth time!",
          (uintptr_t)arg, j);
    j++;
    yield();
  }
}

void context_kload(PCB *pcb, void *entry, void *arg) {
  Area area = {.start = pcb, .end = pcb + 1};
  pcb->cp = kcontext(area, entry, arg);
}

void context_uload(PCB *pcb, const char *filename, char *const argv[],
                   char *const envp[]) {
  Log("loading bin file %s", filename);
  Assert(argv, "argv is NULL when executing %s", filename);
  Assert(envp, "envp is NULL when executing %s", filename);
  reset_fs();
  Area area = {.start = pcb, .end = pcb + 1};
  uintptr_t entry = loader(pcb, filename);
  pcb->cp = ucontext(&(pcb->as), area, (void *)entry);
  uint64_t offsetCount = 0;
  int argc = 0;
  int envc = 0;
  // create stack space
  uint8_t *stack_pages = (uint8_t *)new_page(STACK_SIZE / PGSIZE) - STACK_SIZE;
  for (int i = 0; i < STACK_SIZE / PGSIZE; i++) {
    map(&(pcb->as), pcb->as.area.end - STACK_SIZE + i * PGSIZE,
        stack_pages + i * PGSIZE, 1);
  }
  void *stack = stack_pages + STACK_SIZE;
  for (int i = 0; envp[i]; i++) {
    envc += 1;
    offsetCount += strlen(envp[i]) + 1;
    strcpy(stack - offsetCount, envp[i]);
  }
  for (int i = 0; argv[i]; i++) {
    argc += 1;
    offsetCount += strlen(argv[i]) + 1;
    strcpy(stack - offsetCount, argv[i]);
  }

  int tempOffset = 0;
  while (((uint64_t)stack - offsetCount) % 8) {
    offsetCount++;
  }
  *((uint64_t *)(stack - offsetCount) - 1) = (uint64_t)NULL;
  offsetCount += sizeof(uint64_t);
  for (int i = 0; envp[i]; i++) {
    tempOffset += strlen(envp[i]) + 1;
    *((uint64_t *)(stack - offsetCount) - 1) = (uint64_t)(stack - tempOffset);
    offsetCount += sizeof(uint64_t);
  }
  *((uint64_t *)(stack - offsetCount) - 1) = (uint64_t)NULL;
  offsetCount += sizeof(uint64_t);
  for (int i = 0; argv[i]; i++) {
    tempOffset += strlen(argv[i]) + 1;
    uint64_t *reversedp = (uint64_t *)(stack - offsetCount) - argc + i + i;
    *reversedp = (uint64_t)(stack - tempOffset);
    offsetCount += sizeof(uint64_t);
  }
  *((uint64_t *)(stack - offsetCount) - 1) = argc;
  offsetCount += sizeof(uint64_t);
  pcb->cp->GPRx = (uint64_t)(stack - offsetCount);
}

PCB *getPCB() { return &(pcb[pcbcount++]); }

void init_proc() {
  Log("Initializing processes...");
  char target_program[] = "/bin/nterm";
  executing[0] = getPCB();
  context_kload(executing[0], hello_fun, "p2");
  char *args[] = {target_program, NULL};
  char *envp[] = {NULL};
  executing[1] = getPCB();
  context_uload(executing[1], target_program, args, envp);
  switch_boot_pcb();
}

void replacePCB(PCB *newone) {
  for (int i = 0; i < 2; i++) {
    if (executing[i] == current) {
      executing[i] = newone;
      return;
    }
  }
}

Context *schedule(Context *prev) {
  current->cp = prev;
  Log("jump to proc %d", current == executing[0]);
  current = current == executing[0] ? executing[1] : executing[0];
  return current->cp;
}
