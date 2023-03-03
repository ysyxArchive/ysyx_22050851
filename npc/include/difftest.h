#ifndef __DIFFTEST_H__
#define __DIFFTEST_H__

#include "common.h"

void difftest_check(CPU* cpu);

void difftest_initial(CPU* cpu);

void load_difftest_so(char* diff_so_file);

#define TO_REF 1
#define FROM_REF 0

#endif