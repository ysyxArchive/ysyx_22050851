#include <mem.h>

uint8_t mem[MEM_LEN];
size_t bin_file_size;

void init_memory(char* bin_path) {
  FILE* bin_file = fopen(bin_path, "r");
  Assert(bin_file != NULL, "read bin file error");
  int ptr = 0;
  while (fread(mem + ptr, 4, 1, bin_file)) {
    ptr += 4;
  }
  fclose(bin_file);
  bin_file_size = ptr;
}

uint64_t read_mem(uint64_t addr, size_t length) {
  uint64_t ret = 0;
  Assert(addr >= MEM_START, "addr 0x%lx < MEM_START 0x%x", addr, MEM_START);
  Assert(addr + length <= MEM_START + MEM_LEN,
         "addr 0x%lx + 0x%lx > MEM_END 0x%x", addr, length,
         MEM_START + MEM_LEN);

  if (length == 1) {
    return *((uint8_t*)(mem + (addr - MEM_START)));
  } else if (length == 2) {
    return *((uint16_t*)(mem + (addr - MEM_START)));
  } else if (length == 4) {
    return *((uint32_t*)(mem + (addr - MEM_START)));
  } else if (length == 8) {
    return *((uint64_t*)(mem + (addr - MEM_START)));
  } else {
    panic("length is not allowed, only allowed 1, 2, 4, 8");
  }

  return 0;
}