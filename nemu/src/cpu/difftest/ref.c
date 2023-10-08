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

#include <cpu/cpu.h>
#include <difftest-def.h>
#include <isa.h>
#include <memory/vaddr.h>

#define TO_REF 1
#define FROM_REF 0

typedef struct CPU_host {
  uint64_t gpr[32];
  uint64_t pc;
} CPU_host;

void difftest_memcpy(paddr_t addr, void* buf, size_t n, bool direction) {
  if (direction == TO_REF) {
    int i = 0;
    for (; i < n - 8; i += 8)
      vaddr_write(addr + i, 8, ((uint64_t*)buf)[i / 8]);
    for (; i < n; i++) {
      vaddr_write(addr + i, 1, ((uint8_t*)buf)[i]);
    }
  } else {
    int i = 0;
    for (; i < n - 8; i += 8)
      ((uint64_t*)buf)[i / 8] = vaddr_read(addr + i, 8);
    for (; i < n; i++) {
      ((uint8_t*)buf)[i] = vaddr_read(addr + i, 1);
    }
  }
}

void difftest_regcpy(void* dut, bool direction) {
  CPU_host* cpu_host = dut;
  if (direction == TO_REF) {
    for (int i = 1; i < 32; i++) {
      cpu.gpr[i] = cpu_host->gpr[i];
    }
    cpu.pc = cpu_host->pc;
  } else {
    for (int i = 0; i < 32; i++) {
      cpu_host->gpr[i] = cpu.gpr[i];
    }
    cpu_host->pc = cpu.pc;
  }
}

void difftest_exec(uint64_t n) {
  cpu_exec(n);
}

void difftest_raise_intr(word_t NO) {
  // TODO
}

void difftest_init(int port) {
  /* Perform ISA dependent initialization. */
  init_isa();
}
