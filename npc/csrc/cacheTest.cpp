#include "common.h"

// void cache_

svOpenArrayHandle dcache_valid = NULL;
svOpenArrayHandle dcache_dirty = NULL;
svOpenArrayHandle icache_valid = NULL;
svOpenArrayHandle icache_dirty = NULL;

extern "C" void set_cacheinfo_ptr(svLogic isDCache, const svOpenArrayHandle d,
                                  const svOpenArrayHandle v) {
  if (!isDCache) {
    icache_valid = v;
    icache_dirty = d;
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
    for (int j = 0; j < 16; j++) {
      printf("%x",
             ((bool*)(((VerilatedDpiOpenVar*)icache_valid)->datap()))[j]);
    }
    printf("\n");
    // for (int i = 0; i < 4; i++) {
    //   for (int j = 0; j < 4; j++) {
    //     printf("%x ", (((bool*)icache_valid)[j]));
    //   }
    // }
    // printf("\n");
    // for (int i = 0; i < 4; i++) {
    //   for (int j = 0; j < 4; j++) {
    //     printf("%x ", (((bool*)icache_dirty)[j]));
    //   }
    // }
    // printf("\n");
  } else {
    Log("dcache change");
  }
}