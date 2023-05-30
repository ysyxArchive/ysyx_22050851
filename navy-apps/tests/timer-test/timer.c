#include <NDL.h>
#include <stdint.h>
#include <sys/time.h>

#define SYS_yield 1

int main() {
  uint32_t last = NDL_GetTicks();
  for (int i = 0; i < 100; i++) {
    uint32_t now = NDL_GetTicks();
    while (now - last < 500) {
      now = NDL_GetTicks();
    }
    last = now;
    printf("%d %d %d \n", i, now);
  }
  return _syscall_(SYS_yield, 0, 0, 0);
}
