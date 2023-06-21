#include <NDL.h>

int SDL_Init(uint32_t flags) {
  return NDL_Init(flags);
}

void SDL_Quit() {
  NDL_Quit();
}

char *SDL_GetError() {
  return "Navy does not support SDL_GetError()";
}

int SDL_SetError(const char* fmt, ...) {
    exit(0);
  return -1;
}

int SDL_ShowCursor(int toggle) {
    exit(0);
  return 0;
}

void SDL_WM_SeCaption(const char *title, const char *icon) {
    exit(0);
}
