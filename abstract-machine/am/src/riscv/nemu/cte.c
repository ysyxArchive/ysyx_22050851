#include <am.h>
#include <klib.h>
#include <riscv/riscv.h>
static Context *(*user_handler)(Event, Context *) = NULL;
extern void __am_get_cur_as(Context *c);
extern void __am_switch(Context *c);

Context *__am_irq_handle(Context *c) {
  printf("irqstart\n");
  for (int i = 0 ; i < 32 ; i ++){
    printf("%x ", c->gpr[i]);
  }
  __am_get_cur_as(c);
  printf("callc: %x\n", c);
  if (user_handler) {
    Event ev = {0};
    switch (c->mcause) {
    case 8:
    case 11:
      ev.event = EVENT_YIELD;
      c->mepc += 4;
      c = user_handler(ev, c);
      break;
    case 0x8000000000000007:
      ev.event = EVENT_IRQ_TIMER;
      c = user_handler(ev, c);
      break;
    default:
      printf("unkown error code %x", c->mcause);
      halt(1);
      ev.event = EVENT_ERROR;
      break;
    }
  }
  __am_switch(c);
  printf("irqend\n");
  for (int i = 0 ; i < 32 ; i ++){
    printf("%x ", c->gpr[i]);
  }
  
  return c;
}

extern void __am_asm_trap(void);

bool cte_init(Context *(*handler)(Event, Context *)) {
  // initialize exception entry
  asm volatile("csrw mtvec, %0" : : "r"(__am_asm_trap));
  asm volatile("csrw mscratch, 0");
  // register event handler
  user_handler = handler;

  return true;
}

Context *kcontext(Area kstack, void (*entry)(void *), void *arg) {
  Context c = {.mepc = (uint64_t)entry, .mstatus = 0xa00001880};
  c.GPR2 = (uint64_t)arg;
  memcpy(kstack.end - sizeof(c), &c, sizeof(c));
  return kstack.end - sizeof(c);
}

void yield() { asm volatile("li a7, 1; ecall"); }

bool ienabled() { return false; }

void iset(bool enable) {}
