#include <NDL.h>
#include <sdl-timer.h>
#include <stdio.h>

SDL_TimerID SDL_AddTimer(uint32_t interval, SDL_NewTimerCallback callback,
                         void *param) {
  // FIXME: it may cause problems
  exit(0);
  return NULL;
}

int SDL_RemoveTimer(SDL_TimerID id) {
  exit(0);
  return 1;
}

uint32_t SDL_GetTicks() { return NDL_GetTicks(); }

void SDL_Delay(uint32_t ms) {
  uint32_t starttime = SDL_GetTicks();
  while (SDL_GetTicks() - starttime < ms)
    ;
  return
}
