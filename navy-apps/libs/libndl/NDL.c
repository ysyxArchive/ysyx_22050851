#include <assert.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/time.h>
#include <unistd.h>
static int evtdev = -1;
static int fbdev = -1;
static int screen_w = 0, screen_h = 0;
static int window_w = 0, window_h = 0;
// return ms
uint32_t NDL_GetTicks() {
  struct timeval tv;
  gettimeofday(&tv, 0);
  return tv.tv_sec * 1000 + tv.tv_usec / 1000;
}

int NDL_PollEvent(char *buf, int len) {
  int ret = read(evtdev, buf, len);
  return buf[0] == 'k' && (buf[1] == 'd' || buf[1] == 'u') && buf[2] == ' ';
}

void NDL_OpenCanvas(int *w, int *h) {
  if (*w == 0 && *h == 0) {
    *w = window_w;
    *h = window_h;
  }
  screen_w = *w;
  screen_h = *h;
  if (getenv("NWM_APP")) {
    int fbctl = 4;
    fbdev = 5;
    screen_w = *w;
    screen_h = *h;
    char buf[64];
    int len = sprintf(buf, "%d %d", screen_w, screen_h);
    // let NWM resize the window and create the frame buffer
    write(fbctl, buf, len);
    while (1) {
      // 3 = evtdev
      int nread = read(3, buf, sizeof(buf) - 1);
      if (nread <= 0)
        continue;
      buf[nread] = '\0';
      if (strcmp(buf, "mmap ok") == 0)
        break;
    }
    close(fbctl);
  }
}
typedef struct {
  uint32_t x;
  uint32_t y;
  uint32_t w;
  uint32_t h;
  uint32_t *pixel;
} am_rect;
void NDL_DrawRect(uint32_t *pixels, int x, int y, int w, int h) {
  int left_offset = (window_w - screen_w) / 2;
  int top_offset = (window_h - screen_h) / 2;
  // am_rect rect = {.x = x + left_offset,
  //                 .y = y + top_offset,
  //                 .w = w,
  //                 .h = h,
  //                 .pixel = pixels};
  // write(fbdev, &rect, sizeof(am_rect));
  for (int row = 0; row < h; row++) {
    fseek(fbdev,
          ((top_offset + y + row) * window_w + x + left_offset) *
              sizeof(uint32_t),
          SEEK_SET);
    fwrite(pixels + w * row, sizeof(uint32_t), w, fbdev);
  }
}

void NDL_OpenAudio(int freq, int channels, int samples) {}

void NDL_CloseAudio() {}

int NDL_PlayAudio(void *buf, int len) { return 0; }

int NDL_QueryAudio() { return 0; }

int deal_with_key_value(char *buf, char *key, int *value) {
  int p = 0;
  while (buf[p]) {
    char found = 0;
    int o = 0;
    while (buf[p] != ' ' && buf[p] != ':' && key[o]) {
      if (buf[p] != key[o])
        break;
      p++;
      o++;
    }
    if ((buf[p] == ' ' || buf[p] == ':') && !key[o]) {
      found = 1;
    }
    if (!found) {
      while (buf[p] != '\n') {
        p++;
      }
      p++;
    } else {
      while (buf[p] != ':') {
        p++;
      }
      p++;
      while (buf[p] != ' ') {
        p++;
      }
      sscanf(buf + p, "%d", value);
      return 1;
    }
  }
  return 0;
}

int NDL_Init(uint32_t flags) {
  if (getenv("NWM_APP")) {
    evtdev = 3;
  }
  evtdev = open("/dev/events", "r");
  fbdev = open("/dev/fb", "w");
  // read display info
  int dispConfigFile = fopen("/dev/dispinfo", "r");
  char buf[100];
  int info[2];
  fread(buf, 1, 100, dispConfigFile);
  fclose(dispConfigFile);
  assert(deal_with_key_value(buf, "WIDTH", &window_w));
  assert(deal_with_key_value(buf, "HEIGHT", &window_h));

  return 0;
}

void NDL_Quit() {
  close(evtdev);
  close(fbdev);
}
