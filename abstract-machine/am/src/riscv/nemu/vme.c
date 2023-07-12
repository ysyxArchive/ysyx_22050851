#include <am.h>
#include <klib.h>
#include <nemu.h>
#define BITMASK(bits) ((1ull << (bits)) - 1)
#define BITS(x, hi, lo)                                                        \
  (((x) >> (lo)) & BITMASK((hi) - (lo) + 1)) // similar to x[hi:lo] in verilog
#define PTEVALID(x) BITS(x, 0, 0)
#define PTEXWR(x) BITS(x, 3, 1)
#define PTEPPN(x) (BITS(x, 53, 10) << 12)
static AddrSpace kernel_addr_space = {};
static void *(*pgalloc_usr)(int) = NULL;
static void (*pgfree_usr)(void *) = NULL;
static int vme_enable = 0;

static Area segments[] = { // Kernel memory mappings
    NEMU_PADDR_SPACE};

#define USER_SPACE RANGE(0x40000000, 0x80000000)

static inline void set_satp(void *pdir) {
  uintptr_t mode = 1ul << (__riscv_xlen - 1);
  asm volatile("csrw satp, %0" : : "r"(mode | ((uintptr_t)pdir >> 12)));
}

static inline uintptr_t get_satp() {
  uintptr_t satp;
  asm volatile("csrr %0, satp" : "=r"(satp));
  return satp << 12;
}

bool vme_init(void *(*pgalloc_f)(int), void (*pgfree_f)(void *)) {
  pgalloc_usr = pgalloc_f;
  pgfree_usr = pgfree_f;

  kernel_addr_space.ptr = pgalloc_f(1);

  int i;
  for (i = 0; i < LENGTH(segments); i++) {
    void *va = segments[i].start;
    for (; va < segments[i].end; va += PGSIZE) {
      map(&kernel_addr_space, va, va, 1);
    }
  }

  set_satp(kernel_addr_space.ptr);
  printf("kernel addr dir %p\n", kernel_addr_space.ptr);
  vme_enable = 1;

  return true;
}

void protect(AddrSpace *as) {
  PTE *updir = (PTE *)(pgalloc_usr(1));
  as->ptr = updir;
  as->area = USER_SPACE;
  as->pgsize = PGSIZE;
  // map kernel space
  memcpy(updir, kernel_addr_space.ptr, PGSIZE);
  map(as, updir, updir, 1);
}

void unprotect(AddrSpace *as) {}

void __am_get_cur_as(Context *c) {
  c->pdir = (vme_enable ? (void *)get_satp() : NULL);
}

void __am_switch(Context *c) {
  if (vme_enable && c->pdir != NULL) {
    set_satp(c->pdir);
  }
}
void map(AddrSpace *as, void *va, void *pa, int prot) {
  uint64_t vaint = (uint64_t)va;
  // assert high position is equal
  assert(((((uint64_t)vaint << 1) ^ ((uint64_t)vaint)) & 0xFFFFFF8000000000) ==
         0);
  uint64_t vpn[] = {
      BITS(vaint, 20, 12),
      BITS(vaint, 29, 21),
      BITS(vaint, 38, 30),
  };
  // 二级页表
  if (!PTEVALID(((PTE *)as->ptr)[vpn[2]])) {
    uintptr_t newpage = (uintptr_t)pgalloc_usr(1);
    ((PTE *)as->ptr)[vpn[2]] = (BITS(newpage, 55, 12) << 10) | 1 | 0x11;
  }
  PTE *table1 = (PTE *)PTEPPN(((PTE *)(as->ptr))[vpn[2]]);
  // 三级页表
  if (!PTEVALID(table1[vpn[1]])) {
    uintptr_t newpage = (uintptr_t)pgalloc_usr(1);
    table1[vpn[1]] = (BITS(newpage, 55, 12) << 10) | 1 | 0x11;
  }
  PTE *table2 = (PTE *)PTEPPN(table1[vpn[1]]);
  table2[vpn[0]] = BITS((uintptr_t)pa, 55, 12) << 10 | 0x1F; //1 | (0x7 << 1) | ((!!prot) << 4);
}

Context *ucontext(AddrSpace *as, Area kstack, void *entry) {

  Context c = {
      .mepc = (uint64_t)entry, .mstatus = 0xa000c0000, .pdir = as->ptr};
  memcpy(kstack.start, &c, sizeof(c));
  return kstack.start;
}
