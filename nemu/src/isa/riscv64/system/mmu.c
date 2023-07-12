/***************************************************************************************
 * Copyright (c) 2014-2022 Zihao Yu, Nanjing University
 *
 * NEMU is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan
 *PSL v2. You may obtain a copy of Mulan PSL v2 at:
 *          http://license.coscl.org.cn/MulanPSL2
 *
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY
 *KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 *NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 *
 * See the Mulan PSL v2 for more details.
 ***************************************************************************************/

#include <common.h>
#include <isa.h>
#include <memory/paddr.h>
#include <memory/vaddr.h>
#define PTEVALID(x) BITS(x, 0, 0)
#define PTEPPN(x) (BITS(x, 53, 10) << 12)
typedef uint64_t PTE;
uintptr_t last = 0;
paddr_t isa_mmu_translate(vaddr_t vaddr, int len, int type) {
  if (isa_mmu_check_easy() == MMU_DIRECT)
    return vaddr;
  uint64_t vpn[] = {
      BITS(vaddr, 20, 12),
      BITS(vaddr, 29, 21),
      BITS(vaddr, 38, 30),
  };
  uintptr_t ptentry = BITS(csrs("satp"), 43, 0) << 12;
  if (last != ptentry) {
    printf("ptentry %lx -> %lx\n", last, ptentry);
    last = ptentry;
  }
  Assert(ptentry, "ptentry is NULL");
  PTE pte1 = paddr_read(ptentry + vpn[2] * sizeof(PTE), sizeof(PTE));
  Assert(PTEVALID(pte1),
         "pte level 1 is not available when finding for vaddr %lx, pdir is %lx",
         vaddr, ptentry);
  // 二级页表
  uintptr_t table2 = PTEPPN(pte1);
  Assert(table2, "table2 is NULL");
  PTE pte2 = paddr_read(table2 + vpn[1] * sizeof(PTE), sizeof(PTE));
  Assert(PTEVALID(pte2),
         "pte level 2 is not available when finding for vaddr %lx, pdir is %lx",
         vaddr, ptentry);
  // 三级页表
  uintptr_t table3 = PTEPPN(pte2);
  Assert(table3, "table3 is NULL");
  PTE pte3 = paddr_read(table3 + vpn[0] * sizeof(PTE), sizeof(PTE));
  Assert(PTEVALID(pte3),
         "pte level 3 is not available when finding for vaddr %lx, pdir is %lx",
         vaddr, ptentry);
  if (vaddr < 0x8000000)
    Log("vaddr: %lx -> paddr: %llx", vaddr, PTEPPN(pte3) | BITS(vaddr, 11, 0));
  return PTEPPN(pte3) | BITS(vaddr, 11, 0);
}
