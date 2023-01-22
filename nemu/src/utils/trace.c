#include <common.h>
#include <elf.h>
#include <string.h>
//  iringbuf -------------------------------------------------
#define RING_SIZE 16

char iringbuf[RING_SIZE][50] = {0};
unsigned char iringp = 0;

// copy the source to the ringbuf
void add_inst_to_ring(char* source) {
  strncpy(iringbuf[iringp], source, 49);
  iringp = (iringp + 1) % RING_SIZE;
}

void print_ring_buf() {
  int p = (iringp + 1) % RING_SIZE;
  printf("\nRecently executed instrucitons:\n");
  while (p != iringp) {
    printf("%s\n", iringbuf[p]);
    p = (p + 1) % RING_SIZE;
  }
}

//  mtrace -------------------------------------------------
void mtrace(bool is_read, paddr_t addr, int len, word_t data) {
#ifdef CONFIG_MTRACE
#ifdef CONFIG_MTRACE_RANGE
  if (addr < CONFIG_MTRACE_START ||
      addr >= CONFIG_MTRACE_START + CONFIG_MTRACE_OFFSET) {
    return
  }
#endif
  char buf[100];
  int p = sprintf(buf, "detected memory %s at 0x%08x, the data is ",
                  is_read ? "read " : "write", addr);
  for (int i = 7; i >= 0; i--) {
    if (i >= len) {
      sprintf(buf + p, "   ");
    } else {
      sprintf(buf + p, "%02x ", (char)BITS(data, 8 * i + 7, 8 * i) & 0xFF);
    }
    p += 3;
  }
  Log(ANSI_FMT("%s", ANSI_FG_YELLOW), buf);
#endif
  return;
}

// ftrace ----------------------------------------------------
void ftrace() {}