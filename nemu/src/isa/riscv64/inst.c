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

#include "local-include/reg.h"
#include <cpu/cpu.h>
#include <cpu/ifetch.h>
#include <cpu/decode.h>
#include <tracers.h>

#define Reg(i) gpr(i)
#define Csr(i) csr(i)
#define Mr vaddr_read
#define Mw vaddr_write
extern uint8_t priv_status;

enum {
  TYPE_I, TYPE_U, TYPE_S, TYPE_J,
  TYPE_R, TYPE_B, TYPE_N
};

#define src1R() do { *src1 = Reg(rs1); } while (0)
#define src2R() do { *src2 = Reg(rs2); } while (0)
#define immI() do { *imm = SEXT(BITS(i, 31, 20), 12); } while(0)
#define immU() do { *imm = SEXT(BITS(i, 31, 12), 20) << 12; } while(0)
#define immJ() do { *imm = (SEXT(BITS(i, 31, 31), 1) << 20) | (BITS(i, 19, 12) << 12) | (BITS(i, 20, 20) << 11) | (BITS(i, 30, 21) << 1) ; } while(0)
#define immS() do { *imm = (SEXT(BITS(i, 31, 25), 7) << 5) | BITS(i, 11, 7); } while(0)
#define immB() do { *imm = (SEXT(BITS(i, 31, 31), 1) << 12 | BITS(i, 7
static void decode_operand(Decode *s, int *dest, word_t *src1, word_t *src2, word_t *imm, int type) {
  uint32_t i = s->isa.inst.val;
  int rd  = BITS(i, 11, 7);
  int rs1 = BITS(i, 19, 15);
  int rs2 = BITS(i, 24, 20);
  *dest = rd;
  switch (type) {
    case TYPE_I: src1R();          immI(); break;
    case TYPE_U:                   immU(); break;
    case TYPE_J:                   immJ(); break;
    case TYPE_S: src1R(); src2R(); immS(); break;
    case TYPE_B: src1R(); src2R(); immB(); break;
    case TYPE_R: src1R(); src2R();         break;
  }
}

static int decode_exec(Decode *s) {
  int dest = 0;
  word_t src1 = 0, src2 = 0, imm = 0;
  s->dnpc = s->snpc;

#define INSTPAT_INST(s) ((s)->isa.inst.val)
#define INSTPAT_MATCH(s, name, type, ... /* execute body */ ) { \
  decode_operand(s, &dest, &src1, &src2, &imm, concat(TYPE_, type)); \
  __VA_ARGS__ ; \
}

  INSTPAT_START();
lui    , U
auipc  , U
  
lb     , I
lh     , I
lw     , I
ld     , I
lbu    , I
lhu    , I
lwu    , I
addi   , I
slti   , I
sltiu  , I
xori   , I
ori    , I
andi   , I
slli   , I
srli   , I
srai   , I
jalr   , I
addiw  , I
slliw  , I
srliw  , I
sraiw  , I
csrrw  , I
csrrs  , I
csrrc  , I
csrrwi , I
csrrsi , I
csrrci , I
mret   , I
    word_t mstatus = csrs("mstatus"); 
= ((mstatus & 0xFFFFF0000) | 0x0080 | (BITS(mstatus, 7
    s->dnpc = csrs("mepc"); 
    priv_status = ((mstatus >> 11) & 3); 
    etrace(false, cpu.pc, mstatus);
  );

add    , R
sub    , R
slt    , R
sltu   , R
or     , R
and    , R
xor    , R
addw   , R
subw   , R
mul    , R
mulw   , R
div    , R
divu   , R
rem    , R
remu   , R
remw   , R
remuw  , R
divw   , R
divuw  , R
sllw   , R
srlw   , R
sraw   , R
sll    , R
srl    , R
sra    , R

sb     , S
sh     , S
sw     , S
sd     , S
  
jal    , J
  
beq    , B
bne    , B
blt    , B
bge    , B
bltu   , B
bgeu   , B
  
ecall  , N
ebreak , N
inv    , N
  INSTPAT_END();

  Reg(0) = 0; // reset $zero to 0
#ifdef CONFIG_ITRACE
s->pc, s->dnpc, s->snpc);
#endif
  return 0;
}

int isa_exec_once(Decode *s) {
  s->isa.inst.val = inst_fetch(&s->snpc, 4);
  return decode_exec(s);
}
