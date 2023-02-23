#ifndef _MEM_H_
#define _MEM_H_
#include <common.h>
#include <stdio.h>
#include <stdlib.h>

#define MEM_START 0x80000000
#define MEM_LEN 0x10000

void init_memory(char* bin_path);
uint64_t read_mem(uint64_t addr, size_t length);

#endif