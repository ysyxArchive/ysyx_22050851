#include <common.h>
static Context *do_event(Event e, Context *c) {
  switch (e.event) {
  case EVENT_YIELD:
    Log("Triggered, id = %d", c->GPR1);
    switch (c->GPR1) {
    case EVENT_YIELD:
      Log("Triggered YIELD");
      // yield();
      break;
    case EVENT_SYSCALL:
      Log("Triggered SYSTEMCALL");
      break;
    default:
      Panic("Unhandled triggered ID = %d", e.event);
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
