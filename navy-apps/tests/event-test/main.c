#include <NDL.h>
#include <stdio.h>

int main() {
  NDL_Init(0);
  while (1) {
    char buf[64];
    if (NDL_PollEvent(buf, sizeof(buf))) {
      if (buf[4] != 'O')
        printf("receive event: %s\n", buf);
    }
  }
  return 0;
}
