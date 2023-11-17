#include "common.h"

// void cache_

const svLogic* dcache_valid = NULL;
const svLogic* dcache_dirty = NULL;
const svLogic* icache_valid = NULL;
const svLogic* icache_dirty = NULL;

extern "C" void set_cacheinfo_ptr(svLogic isDCache, const svLogic* d,
                                  const svLogic* v) {
  if (!isDCache) {
    icache_valid = v;
    icache_dirty = d;
     for (int i = 0; i < 4; i++) {
      for (int j = 0; j < 4; j++) {
        printf("%x", icache_valid[i * 4 + j] ? 1 : 0);
      }
    }
    printf("\n");
    for (int i = 0; i < 4; i++) {
      for (int j = 0; j < 4; j++) {
        printf("%x", icache_dirty[i * 4 + j] ? 1 : 0);
      }
    }
    printf("\n");
  } else {
    dcache_valid = v;
    dcache_dirty = d;
  }
}

// ("""import "DPI-C" function void cache_change(input string name);
//     import "DPI-C" function void set_cacheinfo_ptr(input string name, input
//     logic [63:0] d [], input logic [63:0] v []);
void cache_change(svLogic isDCache) {
  if (!isDCache) {
    for (int i = 0; i < 4; i++) {
      for (int j = 0; j < 4; j++) {
        printf("%x", icache_valid[i * 4 + j] ? 1 : 0);
      }
    }
    printf("\n");
    for (int i = 0; i < 4; i++) {
      for (int j = 0; j < 4; j++) {
        printf("%x", icache_dirty[i * 4 + j] ? 1 : 0);
      }
    }
    printf("\n");
  } else {
    Log("dcache change");
  }
}