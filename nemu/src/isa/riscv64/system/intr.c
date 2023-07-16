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

#include <isa.h>
#include <time.h>
#include <tracers.h>
uint8_t priv_status = PRIV_M;
clock_t start = 0;

word_t isa_raise_intr(word_t NO, vaddr_t epc) {
  word_t mstatus = csrs("mstatus");
  csrs("mepc") = cpu.pc;
  csrs("mstatus") = ((mstatus | (priv_status << 11)) // set MPP to priv mode
                     & (BITS(mstatus, 3, 3) << 7))   // set MIE to MPIE
                    & (0xFFFFFFFFFFFFFFF7);          // set MIE 0
  csrs("mcause") = NO;
  priv_status = PRIV_M;
  etrace(true, cpu.pc, mstatus);
  return epc;
}

#define IRQ_TIMER 0x8000000000000007

word_t isa_query_intr() {
  // if (!(csrs("mstatus") & 0x8)) {
  //   return INTR_EMPTY;
  // }
  // clock_t end = clock();
  // double elapsed = ((double)(end - start)) / CLOCKS_PER_SEC; // 计算经过的秒数
  // if (elapsed >= 1) { // 每秒一次的时钟中断
  //   Log("trigger!");
  //   start = end;
  //   cpu.INTR = false;
  //   return IRQ_TIMER;
  // }
  return INTR_EMPTY;
}