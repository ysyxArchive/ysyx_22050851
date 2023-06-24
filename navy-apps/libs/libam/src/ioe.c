#include <am.h>
#include <assert.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <NDL.h>
char buffer[100];
#define SAMESTR(s, consts) !strncmp(s, consts, strlen(consts))
void get_dispinfo(AM_GPU_CONFIG_T *configInfo) {
  int disp = open("/dev/dispinfo", "r");
  assert(disp);
  read(disp, buffer, 100);
  close(disp);
  char *token = strtok(buffer, "\n");
  while (token != NULL) {
    printf("%s\n", token);
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
      if (ret == 1) {
        if (SAMESTR(token, "WIDTH")) {
          configInfo->width = val;
        }
        if (SAMESTR(token, "HEIGHT")) {
          configInfo->height = val;
        }
      }
    }
    token = strtok(NULL, "\n");
  }
  printf("width: %d, height: %d\n", configInfo->width, configInfo->height);
  return;
}

void fbdraw(AM_GPU_FBDRAW_T* fbd) {
  NDL_Init(0);
  NDL_DrawRect(fbd->pixels, fbd->x, fbd->y, fbd->w, fbd->h);
  return;
}

bool ioe_init() { return true; }

void ioe_read(int reg, void *buf) {
  printf("trying to read %d\n", reg);
  switch (reg) {
  case AM_GPU_CONFIG:
    get_dispinfo(buf);
    break;
  default:
    printf("trying to read from %d but not recongized\n", reg);
    break;
  }
}
void ioe_write(int reg, void *buf) {
  switch (reg) {
  case AM_GPU_FBDRAW:
    fbdraw(buf);
    break;
  default:
    printf("trying to write to %d but not recongized\n", reg);
    break;
  }
}
