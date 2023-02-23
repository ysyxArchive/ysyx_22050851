#include <mem.h>

FILE* bin_file;

void init_memory(char* bin_path) {
  bin_file = fopen(bin_path, "r");
  Assert(bin_file != NULL, "read bin file error");
}

uint64_t read_mem(uint64_t addr, size_t length) {
  uint64_t ret = 0;
  Assert(addr >= MEM_START, "addr 0x%lx < MEM_START 0x%x", addr, MEM_START);
  Assert(addr + length <= MEM_START + MEM_LEN,
         "addr 0x%lx + 0x%lx > MEM_END 0x%x", addr, length,
         MEM_START + MEM_LEN);
  fseek(bin_file, addr - MEM_START, SEEK_SET);
  fread(&ret, length, 1, bin_file);
  return ret;
}