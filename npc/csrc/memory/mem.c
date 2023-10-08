#include <mem.h>
<<<<<<< HEAD

uint8_t mem[MEM_LEN] = {0};
size_t bin_file_size;
=======
#include "VCPU.h"
#include "config.h"
#include "device.h"
#include "difftest.h"
uint8_t mem[MEM_LEN] = {0};
size_t bin_file_size;
extern CPU cpu;
extern uint32_t vga_data[VGA_HEIGHT * VGA_WIDTH];
extern VCPU* top;
>>>>>>> npc

void init_memory(char* bin_path) {
  FILE* bin_file = fopen(bin_path, "r");
  Assert(bin_file != NULL, "read bin file error");
<<<<<<< HEAD
  int ptr = 0;
  while (fread(mem + ptr, 4, 1, bin_file)) {
    ptr += 4;
  }
=======
  fseek(bin_file, 0L, SEEK_END);
  uint64_t size = ftell(bin_file);
  Assert(size <= MEM_LEN, "file size %lx larger than mem %x", size, MEM_LEN);
  fseek(bin_file, 0L, SEEK_SET);
  fread(mem, size, 1, bin_file);
>>>>>>> npc
  fclose(bin_file);
  bin_file_size = ptr;
}

uint64_t read_mem(uint64_t addr, size_t length) {
  uint64_t ret = read_mem_nolog(addr, length);
<<<<<<< HEAD

  Log(ANSI_FMT("Reading %d bytes, starts with %lx, data is %lx\n",
               ANSI_FG_YELLOW),
      length, addr, ret);
=======
#ifdef MTRACE
  Log(ANSI_FMT("Reading %d bytes, starts with %lx, data is %lx",
               ANSI_FG_YELLOW),
      length, addr, ret);
#endif
>>>>>>> npc
  return ret;
}
uint64_t read_mem_nolog(uint64_t addr, size_t length) {
  uint64_t ret = 0;
<<<<<<< HEAD
  Assert(addr >= MEM_START, "addr 0x%lx < MEM_START 0x%x", addr, MEM_START);
  Assert(addr + length <= MEM_START + MEM_LEN,
         "addr 0x%lx + 0x%lx > MEM_END 0x%x", addr, length,
         MEM_START + MEM_LEN);
  if (length == 1) {
    ret = *((uint8_t*)(mem + (addr - MEM_START)));
  } else if (length == 2) {
    ret = *((uint16_t*)(mem + (addr - MEM_START)));
  } else if (length == 4) {
    ret = *((uint32_t*)(mem + (addr - MEM_START)));
  } else if (length == 8) {
    ret = *((uint64_t*)(mem + (addr - MEM_START)));
  } else {
    panic("length %d is not allowed, only allowed 1, 2, 4, 8", length);
=======
  if (addr == KBD_ADDR) {
    Assert(length == 4 || length == 8,
           "read KBD_ADDR with length == %ld not allowed", length);
    ret = get_key();
    difftest_skip();
  } else if (addr == VGACTL_ADDR) {
    Assert(length == 4 || length == 8,
           "read VGACTL with length == %ld not allowed", length);
    ret = VGA_WIDTH << 16 | VGA_HEIGHT;
    difftest_skip();
  } else if (addr == RTC_ADDR || addr == RTC_ADDR + 4) {
    Assert(length == 4 || length == 8,
           "read from RTC with length == %ld not allowed", length);
    ret = (uint32_t)(gettime() >> ((addr - RTC_ADDR) * 8));
    difftest_skip();
  } else if (addr >= MEM_START && addr <= MEM_START + MEM_LEN) {
    if (length == 1) {
      ret = *((uint8_t*)(mem + (addr - MEM_START)));
    } else if (length == 2) {
      ret = *((uint16_t*)(mem + (addr - MEM_START)));
    } else if (length == 4) {
      ret = *((uint32_t*)(mem + (addr - MEM_START)));
    } else if (length == 8) {
      ret = *((uint64_t*)(mem + (addr - MEM_START)));
    } else {
      panic("length %ld is not allowed, only allowed 1, 2, 4, 8", length);
    }
  } else {
    if (top->reset)
      return 0;
    panic("read from addr 0x%lx + 0x%lx out of range at pc == %lx", addr,
          length, cpu.pc);
>>>>>>> npc
  }

  return ret;
}

void write_mem(uint64_t addr, size_t length, uint64_t data) {
<<<<<<< HEAD
  Assert(addr >= MEM_START, "addr 0x%lx < MEM_START 0x%x", addr, MEM_START);
  Assert(addr + length <= MEM_START + MEM_LEN,
         "addr 0x%lx + 0x%lx > MEM_END 0x%x", addr, length,
         MEM_START + MEM_LEN);

  Log(ANSI_FMT("writing %lx to %lx, len is %lx\n", ANSI_FG_YELLOW), data, addr,
      length);
  if (length == 1) {
    *((uint8_t*)(mem + (addr - MEM_START))) = (uint8_t)data;
  } else if (length == 2) {
    *((uint16_t*)(mem + (addr - MEM_START))) = (uint16_t)data;
  } else if (length == 4) {
    *((uint32_t*)(mem + (addr - MEM_START))) = (uint32_t)data;
  } else if (length == 8) {
    *((uint64_t*)(mem + (addr - MEM_START))) = (uint64_t)data;
  } else {
    panic("length %d is not allowed, only allowed 1, 2, 4, 8", length);
=======
#ifdef MTRACE
  Log(ANSI_FMT("Writing %d bytes to %lx, data is %lx", ANSI_FG_YELLOW), length,
      addr, data);
#endif
  if (addr >= FB_ADDR && addr <= FB_ADDR + VGA_WIDTH * VGA_HEIGHT * 4) {
    Assert(length == 4 || length == 8, "output to FB with length == %ld, not 4",
           length);
    vga_data[(addr - FB_ADDR) / 4] = data;
    difftest_skip();
  } else if (addr == SYNC_ADDR) {
    Assert(length == 4 || length == 8,
           "output to FBCTL with length == %ld, not 4", length);
    Assert(data == 1, "data %ld not valid for SYNC", data);
    printf("trigger!\n");
    update_vga();
    difftest_skip();
  } else if (addr == SERIAL_PORT) {
    Assert(length == 1, "output to Serial Port with length == %ld, not 1",
           length);
    printf("%c", (char)data);
    difftest_skip();
  } else if (addr >= MEM_START && addr + length <= MEM_START + MEM_LEN) {
    if (length == 1) {
      *((uint8_t*)(mem + (addr - MEM_START))) = (uint8_t)data;
    } else if (length == 2) {
      *((uint16_t*)(mem + (addr - MEM_START))) = (uint16_t)data;
    } else if (length == 4) {
      *((uint32_t*)(mem + (addr - MEM_START))) = (uint32_t)data;
    } else if (length == 8) {
      *((uint64_t*)(mem + (addr - MEM_START))) = (uint64_t)data;
    } else {
      panic("length %ld is not allowed, only allowed 1, 2, 4, 8", length);
    }
  } else {
    if (top->reset)
      return;
    panic("write to addr 0x%lx + 0x%lx out of range", addr, length);
>>>>>>> npc
  }
  return;
}
