#include "device.h"
#include <mem.h>
uint8_t mem[MEM_LEN] = {0};
size_t bin_file_size;

void init_memory(char *bin_path) {
  FILE *bin_file = fopen(bin_path, "r");
  Assert(bin_file != NULL, "read bin file error");
  int ptr = 0;
  while (fread(mem + ptr, 4, 1, bin_file)) {
    printf("%d ", ptr);
    ptr += 4;
  }
  fclose(bin_file);
  bin_file_size = ptr;
}

uint64_t read_mem(uint64_t addr, size_t length) {
  uint64_t ret = read_mem_nolog(addr, length);
  // TODO:mtrace
  //  Log(ANSI_FMT("Reading %d bytes, starts with %lx, data is %lx",
  //               ANSI_FG_YELLOW),
  //      length, addr, ret);
  return ret;
}
uint64_t read_mem_nolog(uint64_t addr, size_t length) {
  uint64_t ret = 0;
  Assert(addr >= MEM_START, "addr 0x%lx < MEM_START 0x%x", addr, MEM_START);
  Assert(addr + length <= MEM_START + MEM_LEN,
         "addr 0x%lx + 0x%lx > MEM_END 0x%x", addr, length,
         MEM_START + MEM_LEN);
  if (addr == RTC_ADDR || addr == RTC_ADDR + 4) {
    Assert(length == 4, "read from RTC with length == %d not allowed", length);
    ret = (uint32_t)(gettime() >> ((addr - RTC_ADDR) * 8));
  } else if (addr >= MEM_START && addr + length <= MEM_START + MEM_LEN) {
    if (length == 1) {
      ret = *((uint8_t *)(mem + (addr - MEM_START)));
    } else if (length == 2) {
      ret = *((uint16_t *)(mem + (addr - MEM_START)));
    } else if (length == 4) {
      ret = *((uint32_t *)(mem + (addr - MEM_START)));
    } else if (length == 8) {
      ret = *((uint64_t *)(mem + (addr - MEM_START)));
    } else {
      panic("length %d is not allowed, only allowed 1, 2, 4, 8", length);
    }
  } else {
    panic("writing to addr 0x%lx + 0x%lx out of range", addr, length);
  }
  return ret;
}

void write_mem(uint64_t addr, size_t length, uint64_t data) {
  if (addr == SERIAL_PORT) {
    Assert(length == 1, "output to Serial Port with length == %d, not 1",
           length);
    printf("%c", data);
  } else if (addr >= MEM_START && addr + length <= MEM_START + MEM_LEN) {
    if (length == 1) {
      *((uint8_t *)(mem + (addr - MEM_START))) = (uint8_t)data;
    } else if (length == 2) {
      *((uint16_t *)(mem + (addr - MEM_START))) = (uint16_t)data;
    } else if (length == 4) {
      *((uint32_t *)(mem + (addr - MEM_START))) = (uint32_t)data;
    } else if (length == 8) {
      *((uint64_t *)(mem + (addr - MEM_START))) = (uint64_t)data;
    } else {
      panic("length %d is not allowed, only allowed 1, 2, 4, 8", length);
    }
  } else {
    panic("read from addr 0x%lx + 0x%lx out of range", addr, length);
  }
  return;
}
