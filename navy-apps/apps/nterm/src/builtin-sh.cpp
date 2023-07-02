#include <SDL.h>
#include <nterm.h>
#include <stdarg.h>
#include <unistd.h>
char buffer[100];
char handle_key(SDL_Event *ev);

static void sh_init() { setenv("PATH", "/bin/:/usr/bin/", 1); }
static void sh_printf(const char *format, ...) {
  static char buf[256] = {};
  va_list ap;
  va_start(ap, format);
  int len = vsnprintf(buf, 256, format, ap);
  va_end(ap);
  term->write(buf, len);
}

static void sh_banner() {
  sh_printf("Built-in Shell in NTerm (NJU Terminal)\n\n");
}

static void sh_prompt() { sh_printf("sh> "); }

static void sh_handle_cmd(const char *cmd) {
  int len = strlen(cmd);
  printf("cmd: %s\n", cmd);
  assert(len < 100);
  strncpy(buffer, cmd, len - 1);
  char *program = strtok(buffer, " ");
  char *argv[100];
  int argc = 0;
  char *p = strtok(buffer, " ");
  int i = 0;
  while (p != NULL) {
    if (i == 0) {
    } else {
      argv[argc++] = p;
      assert(argc < 100);
      printf("builtin: %s\n", p);
    }
    p = strtok(NULL, " ");
    i++;
  }
  argv[argc] = NULL;

  execvp(buffer, argv);
}

void builtin_sh_run() {
  sh_init();
  sh_banner();
  sh_prompt();

  while (1) {
    SDL_Event ev;
    if (SDL_PollEvent(&ev)) {
      if (ev.type == SDL_KEYUP || ev.type == SDL_KEYDOWN) {
        const char *res = term->keypress(handle_key(&ev));
        if (res) {
          sh_handle_cmd(res);
          sh_prompt();
        }
      }
    }
    refresh_terminal();
  }
}
