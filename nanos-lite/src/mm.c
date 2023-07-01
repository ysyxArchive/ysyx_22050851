#include <memory.h>
#include <stdint.h>
#include <stdio.h>
void *pf = NULL;
uint8_t* page_end = NULL;
void *new_page(size_t nr_page) { 
  uint8_t* ret = page_end;
  page_end -= nr_page * PGSIZE;
  printf("from %lx to %lx\n", (uint64_t)ret, (uint64_t)page_end);
  return ret;
}

#ifdef HAS_VME
static void *pg_alloc(int n) { return NULL; }
#endif

void free_page(void *p) { panic("not implement yet"); }

/* The brk() system call handler. */
int mm_brk(uintptr_t brk) { return 0; }

void init_mm() {
  pf = (void *)ROUNDUP(heap.start, PGSIZE);
  page_end = heap.end;
  Log("PGSIZE %d", PGSIZE);
  Log("free physical pages starting from %p", pf);

#ifdef HAS_VME
  vme_init(pg_alloc, free_page);
#endif
}
