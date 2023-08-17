/***************************************************************************************
* Copyright (c) 2014-2022 Zihao Yu, Nanjing University
*
* NEMU is licensed under Mulan PSL v2.
* You can use this software according to the terms and conditions of the Mulan PSL v2.
* You may obtain a copy of Mulan PSL v2 at:
*          http://license.coscl.org.cn/MulanPSL2
*
* THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
* EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
* MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
*
* See the Mulan PSL v2 for more details.
***************************************************************************************/
enum {
  PRIV_U, PRIV_S,
  PRIV_V, PRIV_M,
};

#include <isa.h>
#include <tracers.h>
uint8_t current_status = PRIV_M;

word_t isa_raise_intr(word_t NO, vaddr_t epc) {
  /* TODO: Trigger an interrupt/exception with ``NO''.
   * Then return the address of the interrupt/exception vector.
   */

  etrace(true, cpu.pc, NO, epc);
  csrs("mepc") = cpu.pc;
  csrs("mstatus") = ((csrs("mstatus") | (current_status << 11)) // set MPP to priv mode
                    & (0xFFFFFFFFFFFFFF77))          // set MIE MPIE 0
                    | (BITS(csrs("mstatus"), 3, 3) << 7)   // set MIE to MPIE
                    ;
  csrs("mcause") = NO;
  current_status = PRIV_M;
  etrace(true, cpu.pc, csrs("mstatus"), epc);
  return epc;
}

word_t isa_query_intr() {
  return INTR_EMPTY;
}
