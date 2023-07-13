#include "memory.h"
#include <common.h>
#include <stdint.h>
#include <stdio.h>
extern AddrSpace kernel_addr_space;
void *pf = NULL;
uint8_t *page_end = NULL;
void *new_page(size_t nr_page) {
  uint8_t *ret = page_end;
  page_end -= nr_page * PGSIZE;
  return ret;
}

#ifdef HAS_VME
static void *pg_alloc(int n) {
  void *page_addr = (uint8_t *)new_page(n) - PGSIZE * n;
  memset(page_addr, PGSIZE, 0);
  return page_addr;
}
#endif

void free_page(void *p) { panic("not implement yet"); }

/* The brk() system call handler. */
int mm_brk(uintptr_t brk) {
  Log("brk: %x", (uint32_t)brk);
  return 0;
}

void init_mm() {
  pf = (void *)ROUNDUP(heap.start, PGSIZE);
  page_end = heap.end;
  Log("PGSIZE %d", PGSIZE);
  Log("free physical pages starting from %p", pf);

#ifdef HAS_VME
  vme_init(pg_alloc, free_page);
#endif
}
