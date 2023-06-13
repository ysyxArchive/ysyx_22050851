#include <NDL.h>
#include <SDL.h>
#include <string.h>
#define keyname(k) #k,
static const char *keyname[] = {"NONE", _KEYS(keyname)};
static uint8_t keyVals[sizeof(keyname) / sizeof(char *)];
static char buf[20];

int SDL_PushEvent(SDL_Event *ev) {
  exit(0);
  return 0;
}

int SDL_PollEvent(SDL_Event *event) {
  int ans = NDL_PollEvent(buf, 20);
  bool valid = false;
  if (!ans) {
    return 0;
  }
  event->type = buf[1] == 'u' ? SDL_KEYUP : SDL_KEYDOWN;
  for (int end = 3; buf[end]; end++) {
    if (buf[end] == '\n') {
      buf[end] = 0;
      break;
    }
  }
  for (int i = 1; i < sizeof(keyname) / sizeof(char *); i++) {
    if (strcmp(buf + 3, keyname[i]) == 0) {
      event->key.keysym.sym = i;
      valid = true;
      break;
    }
  }
  if(valid){
    printf("detect valid event %s %d", buf+3, event->key.keysym.sym);
  }
  keyVals[event->key.keysym.sym] = event->type == SDL_KEYDOWN;
  return valid;
}

int SDL_WaitEvent(SDL_Event *event) {
  int ans = NDL_PollEvent(buf, 20);
  while (!SDL_PollEvent(event))
    ;
  return 1;
}

int SDL_PeepEvents(SDL_Event *ev, int numevents, int action, uint32_t mask) {
  exit(0);
  return 0;
}

uint8_t *SDL_GetKeyState(int *numkeys) {
  if (numkeys) {
    *numkeys = sizeof(keyVals) / sizeof(uint8_t);
  }
  return keyVals;
}
