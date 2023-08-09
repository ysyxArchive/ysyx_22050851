#include <SDL2/SDL.h>
#include <common.h>
#include <device.h>
#include <sys/time.h>
static uint64_t start = 0;
SDL_Window *window;
uint32_t vga_data[VGA_HEIGHT * VGA_WIDTH];

void init_device() {
  SDL_Init(SDL_INIT_VIDEO);
  window = SDL_CreateWindow("riscv-npc", SDL_WINDOWPOS_CENTERED,
                            SDL_WINDOWPOS_CENTERED, VGA_WIDTH, VGA_HEIGHT,
                            SDL_WINDOW_SHOWN);
}

void update_vga() {
  SDL_Surface *surface = SDL_GetWindowSurface(window);
  SDL_Surface *screen =
      SDL_CreateRGBSurfaceFrom(vga_data, VGA_WIDTH, VGA_HEIGHT, 32, 4,
                               0xFF000000, 0x00FF0000, 0x0000FF00, 0x000000FF);
  SDL_BlitSurface(screen, NULL, surface, NULL);
  SDL_UpdateWindowSurface(window);
}

uint64_t gettime() {
  struct timeval now;
  if (start == 0) {
    gettimeofday(&now, NULL);
    start = now.tv_sec * 1000000 + now.tv_usec;
  }
  gettimeofday(&now, NULL);
  uint64_t end = now.tv_sec * 1000000 + now.tv_usec;
  return end - start;
}
