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
    for (int j = 0; j < 16; j++) {
      printf("%x ", v[j]);
    }
    printf("\n");
  
  } else {
    Log("dcache change");
  }
}