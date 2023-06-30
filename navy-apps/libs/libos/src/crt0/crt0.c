#include <stdint.h>
#include <stdlib.h>
#include <assert.h>
#include <stdio.h>

int main(int argc, char *argv[], char *envp[]);
extern char **environ;
void call_main(uintptr_t *args) {
  printf("%lx\n", args);
  printf("%lx, %ld\n", args, *((uint64_t*)args));
  int argc = *((uint64_t*)args);
  char **argv = (irq(uint64_t*)args + 1);
  printf("%lx\n", argv);
  printf("%lx\n", argv[0]);
  printf("%s\n", argv[0]);
  environ = (char**)*((uint64_t*)args + argc + 1);
  exit(main(argc, argv, environ));
  assert(0);
}
