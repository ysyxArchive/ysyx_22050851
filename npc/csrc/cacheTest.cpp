#include "common.h"

// void cache_

const svLogicVecVal* dcache_valid = NULL;
const svLogicVecVal* dcache_dirty = NULL;
const svLogicVecVal* icache_valid = NULL;
const svLogicVecVal* icache_dirty = NULL;

extern "C" void set_cacheinfo_ptr(const char* name, const svLogicVecVal* d,
                                  const svLogicVecVal* v) {
  if (strcmp(name, "icache") == 0) {
    icache_valid = v;
    icache_dirty = d;
  } else if (strcmp(name, "dcache") == 0) {
    dcache_valid = v;
    dcache_dirty = d;
  } else {
    panic("unkonown cache name %s", name);
  }
}

// ("""import "DPI-C" function void cache_change(input string name);
//     import "DPI-C" function void set_cacheinfo_ptr(input string name, input
//     logic [63:0] d [], input logic [63:0] v []);
void cache_change(const char* name) {
  if (strcmp(name, "icache") == 0) {
    for (int i = 0; i < 4; i++) {
      for (int j = 0; j < 4; j++) {
        printf("%x", icache_valid[i * 4 + j].aval);
      }
    }
    printf("\n");
    for (int i = 0; i < 4; i++) {
      for (int j = 0; j < 4; j++) {
        printf("%x", icache_dirty[i * 4 + j].aval);
      }
    }
    printf("\n");
  } else if (strcmp(name, "dcache") == 0) {
    Log("dcache change");
  } else {
    panic("unkonown cache name %s", name);
  }
}