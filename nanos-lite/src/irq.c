#include "fs.h"
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

static size_t sys_write(int fd, char *buf, size_t count) {
  if (fd == FD_STDIN) {
    return -1;
  } else if (fd == FD_STDOUT || fd == FD_STDERR) {
    for (size_t c = 0; c < count; c++) {
      putch(buf[c]);
    }
  } else {
    
  }
  return count;
}

static size_t sys_brk(void *addr) { return 0; }

static Context *do_event(Event e, Context *c) {
  switch (e.event) {
  case EVENT_YIELD:
    Log("Triggered, id = %d", c->GPR1);
    switch (c->GPR1) {
    case SYS_exit:
      Log("syscall SYS_exit");
      halt(0);
      break;
    case SYS_yield:
      Log("syscall SYS_yield");
      yield();
      break;
    case SYS_write:
      Log("syscall SYS_write %x %x %x", c->GPR2, c->GPR3, c->GPR4);
      c->GPRx = sys_write(c->GPR2, (char *)c->GPR3, c->GPR4);
      break;
    case SYS_brk:
      Log("syscall SYS_brk %x", c->GPR2);
      c->GPRx = sys_brk((void *)c->GPR2);
      break;
    case SYS_open:
      Log("syscall SYS_open %x %x %x", c->GPR2, c->GPR3, c->GPR4);
      c->GPRx = fs_open((void *)c->GPR2, c->GPR3, c->GPR4);
      break;
    case SYS_close:
      Log("syscall SYS_close %x", c->GPR2);
      c->GPRx = fs_close(c->GPR2);
      break;
    case SYS_read:
      Log("syscall SYS_read %x %x %x", c->GPR2, c->GPR3, c->GPR4);
      c->GPRx = fs_read(c->GPR2, (char *)c->GPR3, c->GPR4);
      break;
    case SYS_lseek:
      Log("syscall SYS_seek %x %x %x", c->GPR2, c->GPR3, c->GPR4);
      c->GPRx = fs_lseek(c->GPR2, c->GPR3, c->GPR4);
      break;

    case -1:
      Log("syscall -1, do nothing");
      break;
    default:
      Panic("Unhandled triggered ID = %d", c->GPR1);
    }
    Log("Return %d", c->GPRx);
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
