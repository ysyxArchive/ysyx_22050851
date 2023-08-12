#ifndef _MEM_H_
#define _MEM_H_
#include <common.h>
#include <stdio.h>
#include <stdlib.h>

#define MEM_START 0x80000000
#define MEM_LEN   0x8000000
#define DEVICE_BASE 0xa0000000
#define MMIO_BASE 0xa0000000

#define SERIAL_PORT (DEVICE_BASE + 0x00003f8)
#define KBD_ADDR (DEVICE_BASE + 0x0000060)
#define RTC_ADDR (DEVICE_BASE + 0x0000048)
#define VGACTL_ADDR (DEVICE_BASE + 0x0000100)
#define SYNC_ADDR (VGACTL_ADDR + 4)
#define AUDIO_ADDR (DEVICE_BASE + 0x0000200)
#define DISK_ADDR (DEVICE_BASE + 0x0000300)
#define FB_ADDR (MMIO_BASE + 0x1000000)
#define AUDIO_SBUF_ADDR (MMIO_BASE + 0x1200000)

void init_memory(char *bin_path);
uint64_t read_mem(uint64_t addr, size_t length);
uint64_t read_mem_nolog(uint64_t addr, size_t length);
void write_mem(uint64_t addr, size_t length, uint64_t data);

extern uint8_t mem[];
extern size_t bin_file_size;

#endif