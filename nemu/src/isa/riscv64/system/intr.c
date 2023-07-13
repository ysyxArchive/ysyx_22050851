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
#include <tracers.h>
uint8_t priv_status = PRIV_M;
word_t isa_raise_intr(word_t NO, vaddr_t epc) {
  word_t mstatus = csrs("mstatus");
  csrs("mepc") = cpu.pc;
  csrs("mstatus") = mstatus >> 8 << 8 | (priv_status << 11);
  csrs("mcause") = priv_status == PRIV_U ? 0x8 : 0xb;
  priv_status = PRIV_M;
  etrace(true, cpu.pc, mstatus);
  return epc;
}

word_t isa_query_intr() { return INTR_EMPTY; }
