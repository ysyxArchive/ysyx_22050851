#include "common.h"

// void cache_

// svOpenArrayHandle dcache_valid = NULL;
// svOpenArrayHandle dcache_dirty = NULL;
// svOpenArrayHandle icache_valid = NULL;
// svOpenArrayHandle icache_dirty = NULL;
void cache_change(svLogic isDCache, const svLogic* d, const svLogic* v) {
  //   if (!isDCache) {
  //     for (int j = 0; j < 16; j++) {
  //       printf("%x ", d[j]);
  //     }
  //     printf("\n");
  //     for (int j = 0; j < 16; j++) {
  //       printf("%x ", v[j]);
  //     }
  //     printf("\n");

  //   } else {
  //     Log("dcache change");
  //   }
}

uint64_t iCacheHit = 0;
uint64_t iCacheMiss = 0;
uint64_t dCacheHit = 0;
uint64_t dCacheMiss = 0;

extern "C" void cache_req(svLogic isDCache, svLogic isHit, svLogic reqWrite) {
  if (!isDCache) {
    if (!isHit) {
      iCacheMiss++;
      // Log("iCache Miss!");
    } else {
      iCacheHit++;
    }
  } else {
    if (!isHit) {
      dCacheMiss++;
      // Log("iCache Miss!");
    } else {
      dCacheHit++;
    }
  }
}

void printCacheRate() {
  Log("iCache hit rate: %.2lf%% (%ld / %ld / %ld)",
    (double)iCacheHit / (iCacheHit + iCacheMiss) * 100, iCacheHit, iCacheMiss,
    (iCacheHit + iCacheMiss));
  Log("dCache hit rate: %.2lf%% (%ld / %ld / %ld)",
    (double)dCacheHit / (dCacheHit + dCacheMiss) * 100, dCacheHit, dCacheMiss,
    (dCacheHit + dCacheMiss));
}

uint64_t pipelineMiss[5] = { 0 };

void pip_info(svLogic ifHalt, svLogic idHalt, svLogic exHalt, svLogic memHalt, svLogic wbHalt) {
  if (wbHalt) {
    pipelineMiss[4]++;
  } else if (memHalt) {
    pipelineMiss[3]++;
  } else if (exHalt) {
    pipelineMiss[2]++;
  } else if (idHalt) {
    pipelineMiss[1]++;
  } else if (ifHalt) {
    pipelineMiss[0]++;
  }
}
