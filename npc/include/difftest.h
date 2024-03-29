#ifndef __DIFFTEST_H__
#define __DIFFTEST_H__

#include "common.h"

bool difftest_check(CPU* cpu);

void difftest_checkmem(CPU* cpu);

void difftest_initial(CPU* cpu);

void load_difftest_so(char* diff_so_file);

void difftest_skip();
#define TO_REF 1
#define FROM_REF 0

#endif