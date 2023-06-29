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


void context_uload(PCB *pcb, const char *filename) {
  Area area = {.start=pcb, .end=pcb + 1};
  uintptr_t entry = loader(pcb, filename);
  pcb->cp = ucontext(NULL, area, (void *)entry);
  const char skiparg[] = "--skip";
  printf("%x", heap.end);
  strcpy(heap.end, skiparg);
  *(uint64_t*)((uint8_t*)(heap.end - strlen(skiparg) - 1)) = (uint64_t)NULL;
  *(uint64_t*)((uint8_t*)(heap.end - strlen(skiparg) - 2)) = (uint64_t)NULL;
  *(uint64_t*)((uint8_t*)(heap.end - strlen(skiparg) - 3)) = (uint64_t)NULL;
  *(uint64_t*)((uint8_t*)(heap.end - strlen(skiparg) - 4)) = (uint64_t)heap.end;
  *(uint64_t*)((uint8_t*)(heap.end - strlen(skiparg) - 5)) = (uint64_t)1;
  pcb->cp->GPRx = (uint64_t)(heap.end - strlen(skiparg) - 5);
  // pcb->cp->GPRx = (uint64_t)(heap.end + strlen(skiparg) - 5);

}

void init_proc() {
  // switch_boot_pcb();

  Log("Initializing processes...");
  
  // context_kload(&(pcb[pcbcount++]), hello_fun, "p1");
  context_kload(&(pcb[pcbcount++]), hello_fun, "p2");
  context_uload(&(pcb[pcbcount++]), "/bin/pal");
  switch_boot_pcb();
  // // load program here
  // naive_uload(NULL, "/bin/menu");
}

Context *schedule(Context *prev) { 
  current->cp  = prev;
  printf("%x %x %x\n", current, (pcb[0]).cp, (pcb[1]).cp);
  current = current == &(pcb[0]) ? &(pcb[1]) : &(pcb[0]);
  // current = &(pcb[1]);
  return current->cp;
}
