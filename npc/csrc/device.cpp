#include <common.h>
#include <sys/time.h>
static uint64_t start = 0;
uint64_t gettime() {
  struct timeval now;
  if (start == 0) {
    gettimeofday(&now, NULL);
    start = now.tv_sec * 1000000 + now.tv_usec;
  }
  gettimeofday(&now, NULL);
  uint64_t end = now.tv_sec * 1000000 + now.tv_usec;
  return end - start;
}