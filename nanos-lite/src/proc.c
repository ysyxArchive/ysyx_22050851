#include <loader.h>
#include <proc.h>
#define MAX_NR_PROC 4

static PCB pcb[MAX_NR_PROC] __attribute__((used)) = {};
static PCB pcb_boot = {};
PCB *current = NULL;
int pcbcount = 0;
void switch_boot_pcb() { current = &pcb_boot; }
int j = 1;

void hello_fun(void *arg) {
  while (1) {
    Log("Hello World from Nanos-lite with arg '%s' for the %dth time!",
        (uintptr_t)arg, j);
    j++;
    yield();
  }
}

void context_kload(void *entry, void *arg) {
  Area area = {.start=&pcb[pcbcount], .end=&pcb[pcbcount + 1]};
  pcb[pcbcount].cp = kcontext(area, entry, arg);
  pcbcount += 1;
}


void init_proc() {
  // switch_boot_pcb();

  Log("Initializing processes...");
  context_kload(hello_fun, "p1");
  context_kload(hello_fun, "p2");
  switch_boot_pcb();
  // // load program here
  // naive_uload(NULL, "/bin/menu");
}

Context *schedule(Context *prev) { 
  current->cp  = prev;
  printf("%x %x %x\n", current, pcb, pcb + 1);
  current = current == &(pcb[0]) ? &(pcb[1]) : &(pcb[0]);
  return current->cp;
}
