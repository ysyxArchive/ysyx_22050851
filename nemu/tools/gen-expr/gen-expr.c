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

#include <assert.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>

// this should be enough
static char buf[65536] = {};
static char code_buf[65536 + 128] = {};  // a little larger than `buf`
static char* code_format =
    "#include <stdio.h>\n"
    "int main() { "
    "  unsigned result = %s; "
    "  printf(\"%%u\", result); "
    "  return 0; "
    "}";
int bufp = 0;
static inline int choose(int border) {
  return rand() % border;
}

static void gen_num(int padding) {
  int max_len = 65535 - padding - bufp;
  char str[12];
  int strlength = sprintf(str, "%d", choose(1e4 + 7));  //随便找的数字
  if (strlength > max_len) {
    str[max_len] = 0;
  }
  strcpy(buf + bufp, str);
  bufp += strlen(str);
}

static void gen(char c) {
  buf[bufp++] = c;
  buf[bufp] = '\0';
}

static void gen_rand_op() {
  const char ops[] = {'+', '-', '*', '/'};
  const int length = sizeof(ops) / sizeof(char);
  gen(ops[choose(length)]);
}

static void gen_rand_expr(int padding) {
  if (padding + bufp > 65530) {
    gen('1');
    return;
  }
  switch (choose(5)) {
    case 0:
      gen_num(padding + 1);
      // gen('U');
      break;
    case 1:
      gen('(');
      gen_rand_expr(padding + 1);
      gen(')');
      break;
    case 2:
      gen(' ');
      gen_rand_expr(padding);
      break;
    case 3:
      gen_rand_expr(padding + 1);
      gen(' ');
      break;
    case 4:
      gen_rand_expr(padding + 5);
      gen_rand_op();
      gen_rand_expr(padding);
      break;
  }
}

int main(int argc, char* argv[]) {
  int seed = time(0);
  srand(seed);
  int loop = 1;
  if (argc > 1) {
    sscanf(argv[1], "%d", &loop);
  }
  int i;
  for (i = 0; i < loop;) {
    bufp = 0;
    gen_rand_expr(0);

    sprintf(code_buf, code_format, buf);

    FILE* fp = fopen("/tmp/.code.c", "w");
    assert(fp != NULL);
    fputs(code_buf, fp);
    fclose(fp);

    int ret = system("gcc /tmp/.code.c -o /tmp/.expr -Werror=div-by-zero");
    if (ret != 0)
      continue;
    fp = popen("/tmp/.expr", "r");
    assert(fp != NULL);

    int result;
    assert(fscanf(fp, "%d", &result) != EOF);
    pclose(fp);

    printf("%u %s\n", result, buf);
    i++;
  }
  return 0;
}
