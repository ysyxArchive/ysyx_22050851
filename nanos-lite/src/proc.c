#include <loader.h>
#include <proc.h>
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


void context_uload(PCB *pcb, const char *filename) {
  Area area = {.start=&pcb[pcbcount], .end=&pcb[pcbcount + 1]};
  uintptr_t entry = loader(pcb, filename);
  pcb[pcbcount].cp = ucontext(NULL, area, (void *)entry);
  Log("donw1");
  pcb[pcbcount].cp->GPRx = (uint64_t)heap.end;
}


void context_kload(PCB *pcb, void *entry, void *arg) {
  Area area = {.start=&pcb[pcbcount], .end=&pcb[pcbcount + 1]};
  pcb[pcbcount].cp = kcontext(area, entry, arg);
}



void init_proc() {
  // switch_boot_pcb();

  Log("Initializing processes...");
  
  // context_kload(&(pcb[pcbcount++]), hello_fun, "p1");
  context_uload(&(pcb[pcbcount++]), "/bin/pal");
  Log("donw1");
  context_kload(&(pcb[pcbcount++]), hello_fun, "p2");
  switch_boot_pcb();
  // // load program here
  naive_uload(NULL, "/bin/menu");
}

Context *schedule(Context *prev) { 
  current->cp  = prev;
  printf("%x %x %x\n", current, pcb, pcb + 1);
  current = current == &(pcb[0]) ? &(pcb[1]) : &(pcb[0]);
  return current->cp;
}
