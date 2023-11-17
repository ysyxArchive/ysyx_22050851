
#ifndef __COMMON_H__
#define __COMMON_H__
#include <debug.h>
#include <macros.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "VCPU.h"
#include "VCPU__Dpi.h"
#include "config.h"
#include "verilated.h"
#include "verilated_dpi.h"
#include "verilated_vcd_c.h"

typedef uint64_t paddr_t;
typedef uint64_t word_t;
typedef long unsigned int size_t;

typedef struct CPU {
  uint64_t gpr[32];
  uint64_t pc;
  uint64_t csr[6];
} CPU;
extern CPU cpu;
extern VCPU* top;
void isa_reg_display();

#endif
