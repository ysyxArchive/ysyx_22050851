#include <common.h>

#if defined(MULTIPROGRAM) && !defined(TIME_SHARING)
#define MULTIPROGRAM_YIELD() yield()
#else
#define MULTIPROGRAM_YIELD()
#endif

#define NAME(key) [AM_KEY_##key] = #key,

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
  ((int *)buf)[0] = gpuConfig.width;
  ((int *)buf)[1] = gpuConfig.height;
  return sizeof(int) * 2;
}

size_t fb_write(const void *buf, size_t offset, size_t len) { return 0; }

void init_device() {
  Log("Initializing devices...");
  ioe_init();
}
