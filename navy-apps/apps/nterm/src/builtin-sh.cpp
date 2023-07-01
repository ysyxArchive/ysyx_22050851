#include <nterm.h>
#include <stdarg.h>
#include <unistd.h>
#include <SDL.h>
char buffer[100];
char handle_key(SDL_Event *ev);

static void sh_init(){
  setenv("PATH", "/bin/", 1);
}
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

static void sh_prompt() {
  sh_printf("sh> ");
}

static void sh_handle_cmd(const char *cmd) {
  int len = strlen(cmd);
  assert(len < 100);
  strncpy(buffer, cmd, len - 1);
  char* program = strtok(buffer, " ");
  char* argv[100];
  int argc = 0;
  char* p;
  while((p = strtok(NULL, " "))){
    argv[argc++] = p;
    assert(argc < 100);
  }
  argv[argc] = NULL;

  printf("input %s, argc: %d, args: ", buffer, argc);
  for(int i = 0; i < argc; i++){
    printf("%s, ", argv[i]);
  }
  printf("\n");
  printf("end of argv %x\n", argv[argc]);
  
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
