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
  AM_GPU_CONFIG_T gpuConfig;
  ioe_read(AM_GPU_CONFIG, &gpuConfig);
  return sprintf(buf, "WIDTH : %d\nHEIGHT:    %d\n", gpuConfig.width,
                 gpuConfig.height);
}

size_t fb_write(void *buf, size_t offset, size_t len) {
  int i = 0;
  printf("%d\n", i++);
  AM_GPU_FBDRAW_T fbdraw;
  printf("%d\n", i++);
  fbdraw.h = 1;
  printf("%d\n", i++);
  fbdraw.w = len / sizeof(uint32_t);
  printf("%d\n", i++);
  fbdraw.pixels = buf;
  printf("%d\n", i++);
  fbdraw.sync = true;
  printf("%d\n", i++);
  fbdraw.x = offset % gpuConfig.width;
  printf("%d\n", i++);
  fbdraw.y = offset / gpuConfig.width;
  printf("%d\n", i++);
  ioe_write(AM_GPU_FBDRAW, &fbdraw);
  printf("%d\n", i++);
  return len;
}

void init_device() {
  Log("Initializing devices...");
  ioe_init();
}
