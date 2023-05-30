#include <stdint.h>
#include <sys/time.h>
#ifdef __ISA_NATIVE__
#error can not support ISA=native
#endif

#define SYS_yield 1

int main() {
  struct timeval tv;
  struct timezone tz;
  gettimeofday(&tv, &tz);
  int last = tv.tv_sec * 1000 + tv.tv_usec;
  int diff = 0;
  for (int i = 0; i < 100; i++) {
    int now = tv.tv_sec * 1000 + tv.tv_usec;
    while (diff < 500) {
      gettimeofday(&tv, &tz);
      now = tv.tv_sec * 1000 + tv.tv_usec;
      diff = now - last;
    }
    now = last;
    printf("%d %d \n", tv.tv_sec, tv.tv_usec);
  }
  return _syscall_(SYS_yield, 0, 0, 0);
}
