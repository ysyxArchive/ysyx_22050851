#include <NDL.h>
#include <am.h>
#include <assert.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/time.h>
char buffer[100];
static int fbdev = -1;
static int fbkbd = -1;
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

void fbdraw(AM_GPU_FBDRAW_T *fbd) {
  if (fbdev == -1) {
    fbdev = open("/dev/fb", "w");
  }
  // printf("x %d y %d w %d h %d\n", fbd->x, fbd->y, fbd->w, fbd->h);
  write(fbdev, fbd, sizeof(fbd));
  return;
}

void get_time(AM_TIMER_UPTIME_T *time) {
  struct timeval tv;
  gettimeofday(&tv, NULL);
  time->us = tv.tv_sec * 100000 + tv.tv_usec;
}

void get_input_keybrd(AM_INPUT_KEYBRD_T* kbd) {
  if(fbkbd == -1){
    fbkbd = open("/dev/events", "r");
  }
  read(fbkbd, buffer, 100);
  // printf("%s", buffer);
  
  kbd->keydown = buffer[1] == 'd';
  int i =3;
  while(buffer[i] && buffer[i] != ' '){
    i++;
  }
  sscanf(buffer+i, "%d", &(kbd->keycode));
}

static void __am_timer_config(AM_TIMER_CONFIG_T *cfg) {
  cfg->present = true;
  cfg->has_rtc = true;
}

bool ioe_init() { return true; }

void ioe_read(int reg, void *buf) {
  switch (reg) {
  case AM_GPU_CONFIG:
    get_dispinfo(buf);
    break;
  case AM_TIMER_CONFIG:
    __am_timer_config(buf);
    break;
  case AM_TIMER_UPTIME:
    get_time(buf);
    break;
  case AM_INPUT_KEYBRD:
    get_input_keybrd(buf);
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
