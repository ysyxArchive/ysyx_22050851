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

#include "local-include/reg.h"
#include <isa.h>

const char* regs[] = {"$0", "ra", "sp",  "gp",  "tp", "t0", "t1", "t2",
                      "s0", "s1", "a0",  "a1",  "a2", "a3", "a4", "a5",
                      "a6", "a7", "s2",  "s3",  "s4", "s5", "s6", "s7",
                      "s8", "s9", "s10", "s11", "t3", "t4", "t5", "t6"};

const char* csrregs[CSR_CNT] = {"mepc", "mstatus", "mcause", "mtvec", "satp", "mscratch"};
void isa_reg_display() {
  printf("gpr:\n");
  for (int i = 0; i < 32; i++) {
    printf("%s: %16lX\t%s", regs[i], cpu.gpr[i], i % 4 == 3 ? "\n" : "");
  }
  printf("csr:\n");
  for (int i = 0; i < CSR_CNT; i++) {
    printf("%s: %16lX\t", csrregs[i], cpu.csr[i]);
  }
  printf("\n");
  printf("pc: %lX\n", cpu.pc);
}

word_t isa_reg_str2val(const char* s, bool* success) {
  *success = false;
  word_t ret = 0;
  if (strcmp(s, "pc") == 0) {
    *success = true;
    ret = cpu.pc;
  }
  for (int i = 0; !*success && i < 32; i++) {
    if (strcmp(s, regs[i]) == 0) {
      *success = true;
      ret = cpu.gpr[i];
    }
  }
  for (int i = 0; !*success && i < CSR_CNT; i++) {
    if (strcmp(s, csrregs[i]) == 0) {
      *success = true;
      ret = cpu.csr[i];
    }
  }
  return ret;
}

int isa_reg_str2index(const char* s) {
  for (int i = 0; i < 32; i++) {
    if (strcmp(s, regs[i]) == 0) {
      return i;
    }
  }
  for (int i = 0; i < CSR_CNT; i++) {
    if (strcmp(s, csrregs[i]) == 0) {
      return i;
    }
  }
  panic("no match reg with %s", s);
}