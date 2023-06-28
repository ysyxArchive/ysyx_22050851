#include <loader.h>
#include "fs.h"
#include <common.h>
#include "proc.h"
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
static size_t sys_brk(void *addr) { return 0; }
#define STRACE
#ifdef STRACE
#define strace(format, ...)                                                    \
  do {                                                                         \
    Log(format, ##__VA_ARGS__);                                                \
  } while (0)
#else
#define strace(...)                                                            \
  do {                                                                         \
  } while (0)
#endif

static Context *do_event(Event e, Context *c) {
  switch (e.event) {
  case EVENT_YIELD:
    switch (c->GPR1) {
    case SYS_exit:
      strace("syscall SYS_exit %d", c->GPR2);
      if(c->GPR2 == 0){
        naive_uload(NULL, "/bin/menu");
      } else { 
        halt(c->GPR2);
      }
      break;
    case SYS_yield:
      strace("syscall SYS_yield");
      // yield();
      return schedule(c);
      break;
    case SYS_write:
      strace("syscall SYS_write %s %x %x", get_file_name(c->GPR2), c->GPR3,
             c->GPR4);
      c->GPRx = fs_write(c->GPR2, (char *)c->GPR3, c->GPR4);
      break;
    case SYS_brk:
      strace("syscall SYS_brk %x", c->GPR2);
      c->GPRx = sys_brk((void *)c->GPR2);
      break;
    case SYS_open:
      strace("syscall SYS_open %s %x %x", c->GPR2, c->GPR3, c->GPR4);
      c->GPRx = fs_open((void *)c->GPR2, c->GPR3, c->GPR4);
      break;
    case SYS_close:
      strace("syscall SYS_close %s", get_file_name(c->GPR2));
      c->GPRx = fs_close(c->GPR2);
      break;
    case SYS_read:
      strace("syscall SYS_read %s %x %x", get_file_name(c->GPR2), c->GPR3,
             c->GPR4);
      c->GPRx = fs_read(c->GPR2, (char *)c->GPR3, c->GPR4);
      break;
    case SYS_lseek:
      strace("syscall SYS_seek %x %x %x", c->GPR2, c->GPR3, c->GPR4);
      c->GPRx = fs_lseek(c->GPR2, c->GPR3, c->GPR4);
      break;
    case SYS_gettimeofday:
      strace("syscall SYS_gettimeofday %x %x", c->GPR2, c->GPR3);
      uint64_t ms;
      ioe_read(AM_TIMER_UPTIME, &ms);
      ((uint64_t *)c->GPR2)[0] = ms / 1000000;
      ((uint64_t *)c->GPR2)[1] = ms % 1000000;
      c->GPRx = 0;
      break;
    case SYS_execve:
      strace("syscall SYS_execve %s %x %x", c->GPR2, c->GPR3, c->GPR4);
      naive_uload(NULL, (char*)c->GPR2);
      break;
    case -1:
      strace("syscall -1, do nothing");
      break;
    default:
      Panic("Unhandled triggered ID = %d", c->GPR1);
    }
    strace("Return %d", c->GPRx);
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
