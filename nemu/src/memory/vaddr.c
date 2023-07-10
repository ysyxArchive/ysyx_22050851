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
#include <tracers.h>
word_t vaddr_ifetch(vaddr_t addr, int len) { return paddr_read(addr, len); }

word_t vaddr_read(vaddr_t addr, int len) {
  word_t data;
  if (isa_mmu_check_easy() == MMU_DIRECT) {
    data = paddr_read(addr, len);
  } else {
    paddr_t paddr = isa_mmu_translate(addr, len, 0);
    Assert(paddr != MEM_RET_FAIL, "paddr translate return fail");
    Assert(paddr == addr, "paddr translate wrong");
    data = paddr_read(paddr, len);
  }
  mtrace(true, addr, len, data);
  return data;
}

void vaddr_write(vaddr_t addr, int len, word_t data) {
  mtrace(false, addr, len, data);
  if (isa_mmu_check_easy() == MMU_DIRECT) {
    paddr_write(addr, len, data);
  } else {
    paddr_t paddr = isa_mmu_translate(addr, len, 0);
    Assert(paddr != MEM_RET_FAIL, "paddr translate return fail");
    Assert(paddr == addr, "paddr translate wrong");
    paddr_write(paddr, len, data);
  }
}
