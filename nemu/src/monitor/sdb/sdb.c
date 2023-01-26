/***************************************************************************************
 * Copyright (c) 2014-2022 Zihao Yu, Nanjing University
 *
 * NEMU is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan
 *PSL v2. You may obtain a copy of Mulan PSL v2 at:
 *          http://license.coscl.org.cn/MulanPSL2
 *
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY
 *KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 *NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 *
 * See the Mulan PSL v2 for more details.
 ***************************************************************************************/

#include "sdb.h"
#include <cpu/cpu.h>
#include <isa.h>
#include <readline/history.h>
#include <readline/readline.h>
#include "memory/paddr.h"
static bool is_batch_mode = false;

void init_regex();
void init_wp_pool();

/* We use the `readline' library to provide more flexibility to read from stdin.
 */
static char* rl_gets() {
  static char* line_read = NULL;

  if (line_read) {
    free(line_read);
    line_read = NULL;
  }

  line_read = readline("(nemu) ");

  if (line_read && *line_read) {
    add_history(line_read);
  }

  return line_read;
}

static int cmd_c(char* args) {
  cpu_exec(-1);
  return 0;
}

static int cmd_q(char* args) {
  return -1;
}

static int cmd_si(char* args) {
  char* arg = strtok(NULL, " ");
  uint32_t step = 1;
  if (arg != NULL) {
    // BUG: 负数
    sscanf(arg, "%u", &step);
  }
  cpu_exec(step);
  return 0;
}

static int cmd_d(char* args) {
  char* arg = strtok(NULL, " ");
  int id;
  if (arg == NULL) {
    printf("wrong usage of d\n");
    return 0;
  };
  sscanf(arg, "%u", &id);
  bool res = free_wp(id);
  if (!res) {
    printf("error when deleting watchpoint %d\n", id);
  }
  return 0;
}

static int cmd_info(char* args) {
  char* arg = strtok(NULL, " ");
  if (arg == NULL) {
    printf("error usage of info\n");
    return 0;
  }
  if (strcmp(arg, "r") == 0) {
    isa_reg_display();
  } else if (strcmp(arg, "w") == 0) {
    list_wp();
  } else {
    printf("error usage of info\n");
  }
  return 0;
}

static int cmd_x(char* args) {
  if (args == NULL) {
    printf("wrong usage of x\n");
    return 0;
  }
  int N;
  int ret = sscanf(strtok(args, " "), "%d", &N);
  if (ret == 0) {
    printf("wrong usage of x\n");
    return 0;
  }
  char* exp = strtok(NULL, " ");
  if (exp == NULL) {
    printf("wrong usage of x\n");
    return 0;
  }
  bool success;
  paddr_t p = expr(exp, &success);
  if (!success) {
    printf("expr error\n");
    return 0;
  }

  for (int i = 0; i < N; i++) {
    printf("%02lX %s%s", paddr_read(p + i, 1), i % 4 == 3 ? " " : "",
           i % 8 == 7 ? "\n" : "");
  }
  if (N % 8 != 0) {
    printf("\n");
  }
  return 0;
}

static int cmd_p(char* args) {
  if (args == NULL) {
    printf("wrong usage of p\n");
    return 0;
  }
  char* exp = args;
  bool s;
  uint32_t res = expr(exp, &s);

  if (!s) {
    printf("error when evaling expression\n");
    return 0;
  }

  printf("%u\n", res);
  return 0;
}

static int cmd_w(char* args) {
  if (args == NULL) {
    printf("wrong usage of w\n");
    return 0;
  }
  char* exp = args;

  int id = new_wp(exp);

  if (id == -1) {
    printf("error when creating watchpoint\n");
    return 0;
  }

  printf("watchpoint created: %d %s\n", id, exp);
  return 0;
}

static int cmd_help(char* args);

static struct {
  const char* name;
  const char* description;
  int (*handler)(char*);
} cmd_table[] = {
    {"help", "Display information about all supported commands", cmd_help},
    {"c", "Continue the execution of the program", cmd_c},
    {"q", "Exit NEMU", cmd_q},
    {"si", "step N lines, default is 1", cmd_si},
    {"info", "print register status or watchpoints", cmd_info},
    {"x", "scan N*4 Bytes starts from EXPR ", cmd_x},
    {"p", "print", cmd_p},
    {"w", "set watchpoint", cmd_w},
    {"d", "delete watchpoint", cmd_d},
};

#define NR_CMD ARRLEN(cmd_table)

static int cmd_help(char* args) {
  /* extract the first argument */
  char* arg = strtok(NULL, " ");
  int i;

  if (arg == NULL) {
    /* no argument given */
    for (i = 0; i < NR_CMD; i++) {
      printf("%s - %s\n", cmd_table[i].name, cmd_table[i].description);
    }
  } else {
    for (i = 0; i < NR_CMD; i++) {
      if (strcmp(arg, cmd_table[i].name) == 0) {
        printf("%s - %s\n", cmd_table[i].name, cmd_table[i].description);
        return 0;
      }
    }
    printf("Unknown command '%s'\n", arg);
  }
  return 0;
}

void sdb_set_batch_mode() {
  is_batch_mode = true;
}

void sdb_mainloop() {
  if (is_batch_mode) {
    cmd_c(NULL);
    return;
  }

  for (char* str; (str = rl_gets()) != NULL;) {
    char* str_end = str + strlen(str);

    /* extract the first token as the command */
    char* cmd = strtok(str, " ");
    if (cmd == NULL) {
      continue;
    }

    /* treat the remaining string as the arguments,
     * which may need further parsing
     */
    char* args = cmd + strlen(cmd) + 1;
    if (args >= str_end) {
      args = NULL;
    }

#ifdef CONFIG_DEVICE
    extern void sdl_clear_event_queue();
    sdl_clear_event_queue();
#endif

    int i;
    for (i = 0; i < NR_CMD; i++) {
      if (strcmp(cmd, cmd_table[i].name) == 0) {
        if (cmd_table[i].handler(args) < 0) {
          return;
        }
        break;
      }
    }

    if (i == NR_CMD) {
      printf("Unknown command '%s'\n", cmd);
    }
  }
}

void init_sdb() {
  /* Compile the regular expressions. */
  init_regex();

  /* Initialize the watchpoint pool. */
  init_wp_pool();
}
