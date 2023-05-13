#include <common.h>
static Context *do_event(Event e, Context *c) {
  switch (e.event) {
  case EVENT_YIELD:
    Log("Triggered\n");
    switch(c->GPR1){
      case EVENT_YIELD:
        Log("Triggered YIELD\n");
      case EVENT_SYSCALL:
        Log("Triggered SYSTEMCALL\n");
    }
    break;
  default:
    Panic("Unhandled event ID = %d", e.event);
  }

  return c;
}

void init_irq(void) {
  Log("Initializing interrupt/exception handler...");
  cte_init(do_event);
}
