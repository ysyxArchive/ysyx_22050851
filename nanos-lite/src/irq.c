#include "errno.h"
#include "fs.h"
#include "memory.h"
#include "proc.h"
#include <common.h>
#include <loader.h>
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
// #define STRACE
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
char *NULLARR[] = {NULL};
static Context *do_event(Event e, Context *c) {
  Log("in event, input context is  %x, ptentry is %x", c, c->pdir);
  switch (e.event) {
  case EVENT_IRQ_TIMER:
    strace("syscall SYS_yield from irq timer");
    c = schedule(c);
    break;
  case EVENT_YIELD:
    switch (c->GPR1) {
    case SYS_exit:
      strace("syscall SYS_exit %d", c->GPR2);
      if (c->GPR2 == 0) {
        PCB *newpcb = getPCB();
        context_uload(newpcb, "/bin/nterm", NULLARR, NULLARR);
        replacePCB(newpcb);
        c =  schedule(c);
      } else {
        Log("exit with error number %d", c->GPR2);
        halt(c->GPR2);
      }
      break;
    case SYS_yield:
      strace("syscall SYS_yield");
      c = schedule(c);
      break;
    case SYS_write:
      strace("syscall SYS_write %s %x %x", get_file_name(c->GPR2), c->GPR3,
             c->GPR4);
      c->GPRx = fs_write(c->GPR2, (char *)c->GPR3, c->GPR4);
      break;
    case SYS_brk:
      strace("syscall SYS_brk %x", c->GPR2);
      c->GPRx = mm_brk(c->GPR2);
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
      int ret = fs_open((char *)c->GPR2, 0, 0);
      if (ret == -ENOENT) {
        c->GPRx = ret;
        break;
      }
      PCB *newpcb = getPCB();
      context_uload(newpcb, (char *)c->GPR2, (char **)c->GPR3,
                    (char **)c->GPR4);
      replacePCB(newpcb);
      c = schedule(c);
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
