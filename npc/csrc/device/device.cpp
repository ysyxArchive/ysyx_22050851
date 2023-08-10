#include <SDL2/SDL.h>
#include <common.h>
#include <device.h>
#include <sys/time.h>
#include <time.h>
static uint64_t start = 0;
static SDL_Window *window;
static SDL_Renderer *renderer = NULL;
static SDL_Texture *texture = NULL;

uint32_t vga_data[VGA_HEIGHT * VGA_WIDTH];

void init_device() {
  SDL_Init(SDL_INIT_VIDEO);
  SDL_CreateWindowAndRenderer(VGA_WIDTH, VGA_HEIGHT, 0, &window, &renderer);
  SDL_SetWindowTitle(window, "riscv-npc");
  texture = SDL_CreateTexture(renderer, SDL_PIXELFORMAT_ARGB8888,
                              SDL_TEXTUREACCESS_STATIC, VGA_WIDTH, VGA_HEIGHT);
  init_keyboard();
}

void update_vga() {
  SDL_UpdateTexture(texture, NULL, vga_data, VGA_WIDTH * sizeof(uint32_t));
  SDL_RenderClear(renderer);
  SDL_RenderCopy(renderer, texture, NULL, NULL);
  SDL_RenderPresent(renderer);
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

#define DEVICE_UPDATE_INTERVAL 1000
extern bool is_halt;
extern bool is_bad_halt;

void update_device() {
  static uint64_t last = 0;
  uint64_t now = gettime();
  if (now - last < DEVICE_UPDATE_INTERVAL) {
    return;
  }
  last = now;

  SDL_Event event;
  while (SDL_PollEvent(&event)) {
    switch (event.type) {
    case SDL_QUIT:
    case SDL_WINDOWEVENT_CLOSE:
      Log("SDL_QUIT");
      is_halt = true;
      is_bad_halt = true;
      break;
    // If a key was pressed
    case SDL_KEYDOWN:
    case SDL_KEYUP:
      uint8_t k = event.key.keysym.scancode;
      bool is_keydown = (event.key.type == SDL_KEYDOWN);
      send_key(k, is_keydown);
      break;
    }
  }
}
