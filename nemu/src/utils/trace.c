#include <common.h>
#include <string.h>

//  iringbuf -------------------------------------------------
#define RING_SIZE 16

char iringbuf[RING_SIZE][50] = {0};
unsigned char iringp = 0;

// copy the source to the ringbuf
void add_inst_to_ring(char* source) {
  strcpy(iringbuf[iringp], source);
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
  char buf[100];
  sprintf(buf, "detected memory %s at 0x%08x, the data is \t",
          is_read ? "read" : "write", addr);
  Log(ANSI_FMT("%s", ANSI_FG_YELLOW), buf);
  for (int i = 7; i >= 0; i--) {
    if (i >= len) {
      printf("   ");
    } else {
      printf("%02x ", (char)BITS(data, 8 * i + 7, 8 * i) & 0xFF);
    }
  }
  printf("\n");
  return;
}