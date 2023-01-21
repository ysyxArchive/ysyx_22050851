#include <string.h>

#define RING_SIZE 16

char iringbuf[RING_SIZE][50] = {0};
unsigned char iringp = 0;

// copy the source to the ringbuf
void add_inst_to_ring(char* source) {
  strcpy(iringbuf[iringp++], source);
}

void print_ring_buf(){

}