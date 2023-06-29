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
  char **argv = (char**)*((uint64_t*)args + 1);
  printf("%s\n", argv[0]);
  environ = (char**)*((uint64_t*)args + 2 + argc);
  exit(main(argc, argv, environ));
  assert(0);
}
