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
paddr_t isa_mmu_translate(vaddr_t vaddr, int len, int type) {
  uint64_t vpn[] = {
      BITS(vaddr, 20, 12),
      BITS(vaddr, 29, 21),
      BITS(vaddr, 38, 30),
  };
  uintptr_t ptentry = BITS(csrs("satp"), 43, 0);
  Assert(ptentry, "ptentry is NULL");
  Log("ptentry %lx, vpn %lx, addr %lx", ptentry, vpn[2], ((PTE *)ptentry)[vpn[2]]);
  PTE pte1 = paddr_read(((PTE *)ptentry)[vpn[2]], sizeof(PTE));
  Assert(PTEVALID(pte1), "pte level 1 is not available");
  Log("%lx", pte1);
  // 二级页表
  PTE *table2 = (PTE *)PTEPPN(pte1);
  Assert(table2, "table2 is NULL");
  Log("%p", table2);
  PTE pte2 = paddr_read(table2[vpn[1]], sizeof(PTE));
  Assert(PTEVALID(pte2), "pte level 2 is not available");
  // 三级页表
  PTE *table3 = (PTE *)PTEPPN(pte2);
  Assert(table3, "table3 is NULL");
  Log("%p", table3);
  PTE pte3 = paddr_read(table3[vpn[0]], sizeof(PTE));
  Assert(PTEVALID(pte3), "pte level 3 is not available");
  return PTEPPN(pte3) | BITS(vaddr, 11, 0);
}
