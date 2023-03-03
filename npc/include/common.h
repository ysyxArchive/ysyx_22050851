
#ifndef __COMMON_H__
#define __COMMON_H__

#include <debug.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "debug.h"
#include "mem.h"
#include "util.h"

typedef uint64_t paddr_t;
typedef uint64_t word_t;
typedef long unsigned int size_t;

typedef struct CPU {
  uint64_t gpr[32];
  uint64_t pc;
} CPU;

#endif
