#include <stdint.h>
#include <stdlib.h>
#include <assert.h>
#include <stdio.h>

int main(int argc, char *argv[], char *envp[]);
extern char **environ;
void call_main(uintptr_t *args) {
  int argc = *((uint64_t*)args);
  char **argv = ((uint64_t*)args + 1);
  environ = (char**)((uint64_t*)args + argc + 1 + 1);
  printf("environnnnnn: %s", *environ);
  exit(main(argc, argv, environ));
  assert(0);
}
