#include <common.h>

#if defined(MULTIPROGRAM) && !defined(TIME_SHARING)
#define MULTIPROGRAM_YIELD() yield()
#else
#define MULTIPROGRAM_YIELD()
#endif

#define NAME(key) [AM_KEY_##key] = #key,
static AM_GPU_CONFIG_T gpuConfig;

static const char *keyname[256]
    __attribute__((used)) = {[AM_KEY_NONE] = "NONE", AM_KEYS(NAME)};

size_t serial_write(const void *buf, size_t offset, size_t len) {
  ((char *)buf)[len] = 0;
  for (int i = 0; i < len; i++) {
    putch(((char *)buf)[i]);
  }
  return len;
}

size_t events_read(void *buf, size_t offset, size_t len) {
  AM_INPUT_KEYBRD_T keyboardEvent;
  ioe_read(AM_INPUT_KEYBRD, &keyboardEvent);
  return sprintf(buf, "%s %s\n", keyboardEvent.keydown ? "kd" : "ku",
                 keyname[keyboardEvent.keycode]);
}

size_t dispinfo_read(void *buf, size_t offset, size_t len) {
  ioe_read(AM_GPU_CONFIG, &gpuConfig);
  return sprintf(buf, "WIDTH : %d\nHEIGHT:    %d\n", gpuConfig.width,
                 gpuConfig.height);
}

size_t fb_write(void *buf, size_t offset, size_t len) {
  AM_GPU_FBDRAW_T fbdraw;
  fbdraw.h = 1;
  fbdraw.w = len / sizeof(uint32_t);
  fbdraw.pixels = buf;
  fbdraw.sync = true;
  fbdraw.x = offset / sizeof(uint32_t) % gpuConfig.width;
  fbdraw.y = offset / sizeof(uint32_t) / gpuConfig.width;
  Log("%d %d %d %d", fbdraw.h, fbdraw.w, fbdraw.x, fbdraw.y);
  ioe_write(AM_GPU_FBDRAW, &fbdraw);
  return len;
}

void init_device() {
  Log("Initializing devices...");
  ioe_init();
}
