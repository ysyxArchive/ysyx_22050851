#include <loader.h>
#include <proc.h>
#include <string.h>
#define MAX_NR_PROC 4

static PCB pcb[MAX_NR_PROC] __attribute__((used)) = {};
static PCB pcb_boot = {};
PCB *current = NULL;
int pcbcount = 0;
void switch_boot_pcb() { current = &pcb_boot; }

void hello_fun(void *arg) {
  int j = 1;
  while (1) {
    Log("Hello World from Nanos-lite with arg '%s' for the %dth time!",
        (uintptr_t)arg, j);
    j++;
    yield();
  }
}

void context_kload(PCB *pcb, void *entry, void *arg) {
  Area area = {.start=pcb, .end=pcb + 1};
  pcb->cp = kcontext(area, entry, arg);
}


void context_uload(PCB *pcb, const char *filename, char *const argv[], char *const envp[]) {
  Area area = {.start=pcb, .end=pcb + 1};
  uintptr_t entry = loader(pcb, filename);
  pcb->cp = ucontext(NULL, area, (void *)entry);
  uint64_t offsetCount = 0;
  int argc = 0;
  int envc = 0;
  for(int i = 0; argv[i]; i++) {
    argc += 1;
    offsetCount += strlen(argv[i] + 1);
    strcpy(heap.end - offsetCount, argv[i]);
  }
  for(int i = 0; envp[i]; i++) {
    envc += 1;
    offsetCount += strlen(envp[i] + 1);  
    strcpy(heap.end - offsetCount, argv[i]);
  }
  memcpy((uint64_t*)(heap.end - offsetCount) - envc, envp, envc * sizeof(uint64_t));
  offsetCount += envc * sizeof(uint64_t);
  memcpy((uint64_t*)(heap.end - offsetCount) - argc, argv, argc * sizeof(uint64_t));
  offsetCount += argc * sizeof(uint64_t);
  offsetCount += sizeof(uint64_t);
  *((uint64_t*)(heap.end - offsetCount)) = argc;
  pcb->cp->GPRx = (uint64_t)(heap.end - offsetCount);
  

  // for(int i = 0; argv[i]; i++) {
  //   argc += 1;
  //   offsetCount += strlen(argv[i] + 1);
  //   strcpy(heap.end - offsetCount, argv[i]);
  // }
  
  // const char* skiparg = "--skip";
  // strcpy(heap.end - strlen(skiparg) - 1, skiparg);
  // *((uint64_t*)(heap.end - strlen(skiparg) - 1) - 1) = (uint64_t)NULL;
  // *((uint64_t*)(heap.end - strlen(skiparg) - 1) - 2) = (uint64_t)NULL;
  // *((uint64_t*)(heap.end - strlen(skiparg) - 1) - 3) = (uint64_t)(heap.end - strlen(skiparg) - 1);
  // *((uint64_t*)(heap.end - strlen(skiparg) - 1) - 4) = 1;
  // pcb->cp->GPRx = (uint64_t)((uint64_t*)(heap.end - strlen(skiparg) - 1) - 4);
  // pcb->cp->GPRx = (uint64_t)heap.end;

}

void init_proc() {
  // switch_boot_pcb();

  Log("Initializing processes...");
  
  // context_kload(&(pcb[pcbcount++]), hello_fun, "p1");
  context_kload(&(pcb[pcbcount++]), hello_fun, "p2");
  char* args[] = {"--skip", NULL};
  char* envp[] = {NULL};
  context_uload(&(pcb[pcbcount++]), "/bin/pal", args, envp);
  switch_boot_pcb();
  // // load program here
  // naive_uload(NULL, "/bin/menu");
}

Context *schedule(Context *prev) { 
  current->cp  = prev;
  current = current == &(pcb[0]) ? &(pcb[1]) : &(pcb[0]);
  // current = &(pcb[1]);
  return current->cp;
}
