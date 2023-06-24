#include <am.h>

bool ioe_init() {
  return true;
}

void ioe_read (int reg, void *buf) {
  switch (reg)
  {
  default:
    printf("tryint to read from %d but not recongized\n", reg);
    break;
  }
 }
void ioe_write(int reg, void *buf) { 
  switch (reg)
  {
  default:
    printf("tryint to write to %d but not recongized\n", reg);
    break;
  }
}
