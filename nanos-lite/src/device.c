#include <common.h>
#if defined(MULTIPROGRAM) && !defined(TIME_SHARING)
#define MULTIPROGRAM_YIELD() yield()
#else
#define MULTIPROGRAM_YIELD()
#endif

#define NAME(key) [AM_KEY_##key] = #key,
static AM_GPU_CONFIG_T gpuConfig;
extern int fppcb;
static const char* keyname[256]
    __attribute__((used)) = {[AM_KEY_NONE] = "NONE", AM_KEYS(NAME)};

size_t serial_write(const void* buf, size_t offset, size_t len) {
  // yield();
  ((char*)buf)[len] = 0;
  for (int i = 0; i < len; i++) {
    putch(((char*)buf)[i]);
  }
  return len;
}

size_t events_read(void* buf, size_t offset, size_t len) {
  // yield();
  AM_INPUT_KEYBRD_T keyboardEvent;
  ioe_read(AM_INPUT_KEYBRD, &keyboardEvent);
  if (keyboardEvent.keydown &&
      strcmp(keyname[keyboardEvent.keycode], "F1") == 0) {
    fppcb = 2;
  } else if (keyboardEvent.keydown &&
             strcmp(keyname[keyboardEvent.keycode], "F2") == 0) {
    fppcb = 3;
  }
  return sprintf(buf, "%s %s %d\n", keyboardEvent.keydown ? "kd" : "ku",
                 keyname[keyboardEvent.keycode], keyboardEvent.keycode);
}

size_t dispinfo_read(void* buf, size_t offset, size_t len) {
  // yield();
  ioe_read(AM_GPU_CONFIG, &gpuConfig);
  return sprintf(buf, "WIDTH : %d\nHEIGHT:    %d\n", gpuConfig.width,
                 gpuConfig.height);
}

size_t fb_write(AM_GPU_FBDRAW_T* buf, size_t offset, size_t len) {
  // yield();
  ioe_write(AM_GPU_FBDRAW, buf);
  return len;
}

void init_device() {
  Log("Initializing devices...");
  ioe_init();
}
