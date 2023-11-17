#include "common.h"

// void cache_

svOpenArrayHandle dcache_valid = NULL;
svOpenArrayHandle dcache_dirty = NULL;
svOpenArrayHandle icache_valid = NULL;
svOpenArrayHandle icache_dirty = NULL;
void cache_change(svLogic isDCache, const svLogic* d, const svLogic* v) {
  if (!isDCache) {
    for (int j = 0; j < 16; j++) {
      printf("%x ", d[j]);
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