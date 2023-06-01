#include <NDL.h>
#include <SDL.h>

#define keyname(k) #k,

static const char *keyname[] = {"NONE", _KEYS(keyname)};

static char buf[20];

int SDL_PushEvent(SDL_Event *ev) { return 0; }

int SDL_PollEvent(SDL_Event *ev) { return 0; }

int SDL_WaitEvent(SDL_Event *event) {
  int ans = NDL_PollEvent(buf, 20);
  if (!ans) {
    return ans;
  }
  event->type = buf[1] == 'u' ? SDL_KEYUP : SDL_KEYDOWN; 
  event->key.keysym.sym = 
  return 1;
}

int SDL_PeepEvents(SDL_Event *ev, int numevents, int action, uint32_t mask) {
  return 0;
}

uint8_t *SDL_GetKeyState(int *numkeys) { return NULL; }
