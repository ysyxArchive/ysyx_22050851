#include "common.h"

// void cache_

bool* dcache_valid = NULL;
bool* dcache_dirty = NULL;
bool* icache_valid = NULL;
bool* icache_dirty = NULL;

extern "C" void set_cacheinfo_ptr(const char* name, const svOpenArrayHandle d,
                                  const svOpenArrayHandle v) {
  if (strcmp(name, "iCache") == 0) {
    icache_valid = (bool*)v;
    icache_dirty = (bool*)d;
  } else if (strcmp(name, "dCache") == 0) {
    dcache_valid = (bool*)v;
    dcache_dirty = (bool*)d;
  } else {
    panic("unkonown cache name %s", name);
  }
}

// ("""import "DPI-C" function void cache_change(input string name);
//     import "DPI-C" function void set_cacheinfo_ptr(input string name, input
//     logic [63:0] d [], input logic [63:0] v []);
void cache_change(const char* name) {
  if (strcmp(name, "iCache") == 0) {
    Log("icache change");
  } else if (strcmp(name, "dCache") == 0) {
    Log("dcache change");
  } else {
    panic("unkonown cache name %s", name);
  }
}