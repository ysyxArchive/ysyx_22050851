#include <am.h>
#include <nemu.h>

#define SYNC_ADDR (VGACTL_ADDR + 4)
int width, height;
void __am_gpu_init() {
  int i;
  uint32_t data = inl(VGACTL_ADDR);
  width = (data >> 16) & 0xFFFF;
  height = data & 0xFFFF;
  uint32_t* fb = (uint32_t*)(uintptr_t)FB_ADDR;
  int a;
  a = data;
  while (a > 0) {
    putch(a % 10 + '0');
    a /= 10;
  }
  putch('\n');
  a = width;
  while (a > 0) {
    putch(a % 10 + '0');
    a /= 10;
  }
  putch('\n');

  a = height;
  while (a > 0) {
    putch(a % 10 + '0');
    a /= 10;
  }
  for (i = 0; i < width * height; i++) fb[i] = i;
  outl(SYNC_ADDR, 1);
}

void __am_gpu_config(AM_GPU_CONFIG_T* cfg) {
  *cfg = (AM_GPU_CONFIG_T){.present = true,
                           .has_accel = false,
                           .width = width,
                           .height = height,
                           .vmemsz = 0};
}

void __am_gpu_fbdraw(AM_GPU_FBDRAW_T* ctl) {
  for (int j = 0; j < ctl->h; j++) {
    for (int i = 0; i < ctl->w; i++) {
      outl(FB_ADDR + (ctl->x + i + (ctl->y + j) * width) * 4,
           ((uint32_t*)ctl->pixels)[i + j * ctl->w]);
    }
  }
  if (ctl->sync) {
    outl(SYNC_ADDR, 1);
  }
}

void __am_gpu_status(AM_GPU_STATUS_T* status) { status->ready = true; }
