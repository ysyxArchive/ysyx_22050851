#include <common.h>

enum {
  SYS_exit,
  SYS_yield,
  SYS_open,
  SYS_read,
  SYS_write,
  SYS_kill,
  SYS_getpid,
  SYS_close,
  SYS_lseek,
  SYS_brk,
  SYS_fstat,
  SYS_time,
  SYS_signal,
  SYS_execve,
  SYS_fork,
  SYS_link,
  SYS_unlink,
  SYS_wait,
  SYS_times,
  SYS_gettimeofday
};

static Context *do_event(Event e, Context *c) {
  switch (e.event) {
  case EVENT_YIELD:
    Log("Triggered, id = %d", c->GPR1);
    switch (c->GPR1) {
    case SYS_yield:
      Log("Triggered YIELD");
      yield();
      break;
    default:
      Panic("Unhandled triggered ID = %d", c->GPR1);
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
