#include <stdint.h>
#include <sys/time.h>
#ifdef __ISA_NATIVE__
#error can not support ISA=native
#endif

#define SYS_yield 1

int main() {
  struct timeval tv;
  gettimeofday(&tv, 0);
  int last = tv.tv_sec * 1000 + tv.tv_usec;
  int diff = 0;
  for (int i = 0; i < 100; i++) {
    int now = tv.tv_sec * 1000 + tv.tv_usec;
    // while (diff < 500) {
      // gettimeofday(&tv, 0);
      // now = tv.tv_sec * 1000 + tv.tv_usec;
      // diff = now - last;
    //   printf("%d %d %d \n", i, tv.tv_sec, tv.tv_usec);
    // }
    // now = last;
    printf("%d %d %d \n", i, tv.tv_sec, tv.tv_usec);
  }
  return _syscall_(SYS_yield, 0, 0, 0);
}
