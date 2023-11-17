#include "common.h"

// void cache_

bool* dcache_valid = NULL;
bool* dcache_dirty = NULL;
bool* icache_valid = NULL;
bool* icache_dirty = NULL;

extern "C" void set_cacheinfo_ptr(const char* name, const svOpenArrayHandle d,
                                  const svOpenArrayHandle v) {
  if (strcmp(name, "icache") == 0) {
    icache_valid = (bool*)(((VerilatedDpiOpenVar*)v)->datap());
    icache_dirty = (bool*)(((VerilatedDpiOpenVar*)d)->datap());
  } else if (strcmp(name, "dcache") == 0) {
    dcache_valid = (bool*)(((VerilatedDpiOpenVar*)v)->datap());
    dcache_dirty = (bool*)(((VerilatedDpiOpenVar*)d)->datap());
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
        printf("%d", icache_valid[i * 4 + j]);
      }
    }
    printf("\n");
    for (int i = 0; i < 4; i++) {
      for (int j = 0; j < 4; j++) {
        printf("%d", icache_dirty[i * 4 + j]);
      }
    }
    printf("\n");
  } else if (strcmp(name, "dcache") == 0) {
    Log("dcache change");
  } else {
    panic("unkonown cache name %s", name);
  }
}