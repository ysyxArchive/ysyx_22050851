#include <am.h>
#include <nemu.h>

#define SYNC_ADDR (VGACTL_ADDR + 4)
int width, height;
void __am_gpu_init() {
  int i;
  width = inl(VGACTL_ADDR) >> 16;
  height = inl(VGACTL_ADDR) & 0xFFFF;
  uint32_t *fb = (uint32_t *)(uintptr_t)FB_ADDR;
  for (i = 0; i < width * height; i++)
    fb[i] = i;
  outl(SYNC_ADDR, 1);
}

void __am_gpu_config(AM_GPU_CONFIG_T *cfg) {
  *cfg = (AM_GPU_CONFIG_T){.present = true,
                           .has_accel = false,
                           .width = width,
                           .height = height,
                           .vmemsz = 0};
}

void __am_gpu_fbdraw(AM_GPU_FBDRAW_T *ctl) {
  uint64_t offset = 0;
  uint64_t offset2 = ctl->y * width;
  for (int j = 0; j < ctl->h; j++) {
    for (int i = 0; i < ctl->w; i+=2) {
      outd(FB_ADDR + ((ctl->x + i + offset2) << 2),
           *(uint64_t*)((uint32_t*)ctl->pixels + offset + i));
    }
    offset += ctl->w;
    offset2 += width;
  }
  if (ctl->sync) {
    outl(SYNC_ADDR, 1);
  }
}

void __am_gpu_status(AM_GPU_STATUS_T *status) { status->ready = true; }
