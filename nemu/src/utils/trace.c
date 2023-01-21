#include <common.h>
#include <string.h>

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
  printf("Recently executed instrucitons:\n");
  while (p != iringp) {
    printf("%s\n", iringbuf[p]);
    p = (p + 1) % RING_SIZE;
  }
}