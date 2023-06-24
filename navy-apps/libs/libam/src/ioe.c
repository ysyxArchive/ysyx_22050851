#include <am.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
char buffer[100];
#define SAMESTR(s, consts) !strncmp(s, consts, strlen(consts))
void get_dispinfo(AM_GPU_CONFIG_T* configInfo) {
  int disp = open("/dev/dispinfo", "r");
  assert(disp);
  read(disp, buffer, 100);
  close(disp);
  char *token = strtok(buffer, "\n");
  while (token != NULL) {
    int i = 0;
    int keyend = 0;
    for (; token[i]; i++) {
      if (token[i] == ' ' || token[i] == ':') {
        keyend = i;
        break;
      }
    }
    if (keyend) {
      while (token[i] == ' ' || token[i] == ':') {
        i++;
      }
      int val = 0;
      int ret = sscanf(token + i, "%d", &val);
      if(ret == 1){
        if(SAMESTR(token, "WIDTH")){
          configInfo->width = val;
        }
        if(SAMESTR(token, "HEIGHT")){
          configInfo->height = val;
        }
      }
    }
  }
  return 0;
}

bool ioe_init() { return true; }

void ioe_read(int reg, void *buf) {
  switch (reg) {
    case AM_GPU_CONFIG:
      get_dispinfo(buf);
      break;
  default:
    printf("tryint to read from %d but not recongized\n", reg);
    break;
  }
}
void ioe_write(int reg, void *buf) {
  switch (reg) {
  default:
    printf("tryint to write to %d but not recongized\n", reg);
    break;
  }
}
