
#ifndef __COMMON_H__
#define __COMMON_H__
#include "config.h"
#include "mem.h"
#include "util.h"
#include <debug.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

typedef uint64_t paddr_t;
typedef uint64_t word_t;
typedef long unsigned int size_t;

typedef struct CPU {
  uint64_t gpr[32];
  uint64_t pc;
  uint64_t csr[6];
} CPU;
extern CPU cpu;
void isa_reg_display();

#endif
