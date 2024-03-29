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

#include <isa.h>

/* We use the POSIX regex functions to process regular expressions.
 * Type 'man regex' for more information about POSIX regex functions.
 */
#include <regex.h>
#include "memory/paddr.h"

enum {
  TK_NOTYPE = 256,
  TK_KUOL,
  TK_KUOR,
  TK_HEXNUMBER,
  TK_OCTNUMBER,
  TK_REG,
  TK_POSITIVE,
  TK_NEGATIVE,
  TK_MUL,
  TK_DIV,
  TK_MOD,
  TK_ADD,
  TK_MINUS,
  TK_EQ,
  TK_NEQ,
  TK_AND,
  TK_DEREF
};

// order reference: https://en.cppreference.com/w/c/language/operator_precedence
static struct {
  int length;
  short TKS[3];
} priors[] = {
    {.length = 3, .TKS = {TK_NOTYPE, TK_KUOL, TK_KUOR}},
    // 以上不参与运算
    {.length = 3, .TKS = {TK_OCTNUMBER, TK_HEXNUMBER, TK_REG}},
    // 以上不参与分割
    {.length = 3, .TKS = {TK_POSITIVE, TK_NEGATIVE, TK_DEREF}},
    {.length = 3, .TKS = {TK_MUL, TK_DIV, TK_MOD}},
    {.length = 2, .TKS = {TK_ADD, TK_MINUS}},
    {.length = 3, .TKS = {TK_EQ, TK_NEQ, TK_AND}},
};
const uint32_t priorlen = ARRLEN(priors);

static struct rule {
  const char* regex;
  int token_type;
} rules[] = {
    {" +", TK_NOTYPE},
    {"\\+", '+'},
    {"-", '-'},
    {"\\*", '*'},
    {"/", TK_DIV},
    {"\%", TK_MOD},
    {"==", TK_EQ},
    {"!=", TK_NEQ},
    {"&&", TK_AND},
    {"\\(", TK_KUOL},
    {"\\)", TK_KUOR},
    {"\\$[a-z\\$0-9]{2,3}", TK_REG},
    {"0x[0-9a-fA-F]+", TK_HEXNUMBER},
    {"[0-9]+", TK_OCTNUMBER},
};

#define NR_REGEX ARRLEN(rules)

static regex_t re[NR_REGEX] = {};

/* Rules are used for many times.
 * Therefore we compile them only once before any usage.
 */
void init_regex() {
  int i;
  char error_msg[128];
  int ret;

  for (i = 0; i < NR_REGEX; i++) {
    ret = regcomp(&re[i], rules[i].regex, REG_EXTENDED);
    if (ret != 0) {
      regerror(ret, &re[i], error_msg, 128);
      panic("regex compilation failed: %s\n%s", error_msg, rules[i].regex);
    }
  }
}

typedef struct token {
  int type;
  char str[32];
} Token;

#define MAX_TOKENS 100000
static Token tokens[MAX_TOKENS] __attribute__((used)) = {};
static int nr_token __attribute__((used)) = 0;

static bool should_be_single() {
  return nr_token == 0 || tokens[nr_token - 1].type == TK_MINUS ||
         tokens[nr_token - 1].type == TK_ADD ||
         tokens[nr_token - 1].type == TK_MUL ||
         tokens[nr_token - 1].type == TK_DIV ||
         tokens[nr_token - 1].type == TK_MOD ||
         tokens[nr_token - 1].type == TK_KUOL ||
         tokens[nr_token - 1].type == TK_EQ ||
         tokens[nr_token - 1].type == TK_NEQ ||
         tokens[nr_token - 1].type == TK_AND ||
         tokens[nr_token - 1].type == TK_DEREF;
}

static bool make_token(char* e) {
  int position = 0;
  int i;
  regmatch_t pmatch;
  nr_token = 0;

  while (e[position] != '\0') {
    /* Try all rules one by one. */
    for (i = 0; i < NR_REGEX; i++) {
      if (regexec(&re[i], e + position, 1, &pmatch, 0) == 0 &&
          pmatch.rm_so == 0) {
        char* substr_start = e + position;
        int substr_len = pmatch.rm_eo;

        tokens[nr_token].type = rules[i].token_type;
        strncpy(tokens[nr_token].str, substr_start, substr_len);
        tokens[nr_token].str[substr_len] = '\0';

        position += substr_len;

        switch (tokens[nr_token].type) {
          case TK_NOTYPE:
            // skip empty
            nr_token--;
            break;
          case '+':
            tokens[nr_token].type = should_be_single() ? TK_POSITIVE : TK_ADD;
            break;
          case '-':
            tokens[nr_token].type = should_be_single() ? TK_NEGATIVE : TK_MINUS;
            break;
          case '*':
            tokens[nr_token].type = should_be_single() ? TK_DEREF : TK_MUL;
            break;
        }
        nr_token++;
        Assert(nr_token < MAX_TOKENS, "Too much token in expr");
        break;
      }
    }

    if (i == NR_REGEX) {
      printf("no match at position %d\n%s\n%*.s^\n", position, e, position, "");
      return false;
    }
  }
  return true;
}

bool check_parentheses(int start, int end) {
  int partial_layer = 0;
  for (int i = start; i < end; i++) {
    if (tokens[i].type == TK_KUOL) {
      partial_layer++;
    } else if (tokens[i].type == TK_KUOR) {
      partial_layer--;
    }
    if (partial_layer < 0)
      return false;
  }
  return true;
}

uint32_t eval(int start, int end, bool* success) {
  if (start >= end) {
    *success = false;
    return -1;
  }
  if (start == end - 1) {
    // must be a number
    int retvalue = 0;
    switch (tokens[start].type) {
      bool s;
      case TK_OCTNUMBER:
        sscanf(tokens[start].str, "%u", &retvalue);
        break;
      case TK_HEXNUMBER:
        sscanf(tokens[start].str, "%x", &retvalue);
        break;
      case TK_REG:
        retvalue = isa_reg_str2val(tokens[start].str + 1, &s);
        if (!s) {
          printf("register %s is unknown\n", tokens[start].str + 1);
          retvalue = -1;
          *success = false;
          return -1;
        }
        break;
      default:
        printf(
            "eval error at token index %d, should be a number or a register "
            "but actually not.\n",
            start);
        *success = false;
        return -1;
    }
    *success = true;
    return retvalue;
  }
  if (!check_parentheses(start, end)) {
    printf("parentheses not paired in token bound %d, %d\n", start, end);
    *success = false;
    return -1;
  }
  if (tokens[start].type == TK_KUOL && tokens[end - 1].type == TK_KUOR &&
      check_parentheses(start + 1, end - 1)) {
    return eval(start + 1, end - 1, success);
  }
  int spindex = -1;
  int sppriority = -1;
  int layer = 0;
  for (int i = start; i < end; i++) {
    if (tokens[i].type == TK_KUOL) {
      layer++;
      continue;
    }
    if (tokens[i].type == TK_KUOR) {
      layer--;
      continue;
    }
    if (layer > 0)
      continue;
    bool found = false;
    for (int p = 0; p < priorlen && !found; p++) {
      for (int o = 0; o < priors[p].length && !found; o++) {
        if (tokens[i].type == priors[p].TKS[o]) {
          if (p >= sppriority) {
            sppriority = p;
            spindex = i;
          }
          found = true;
        }
      }
    }
    Assert(found, "token code %d not found priority when evaling\n",
           tokens[i].type);
  }
  // found the index, evaling left part and right part
  uint32_t leftval, rightval;
  bool singleOp = tokens[spindex].type == TK_POSITIVE ||
                  tokens[spindex].type == TK_NEGATIVE ||
                  tokens[spindex].type == TK_DEREF;
  // no left val
  leftval = singleOp ? -1 : eval(start, spindex, success);
  rightval = eval(spindex + 1, end, success);
  if (!*success)
    return -1;
  *success = true;
  switch (tokens[spindex].type) {
    case TK_POSITIVE:
      return rightval;
    case TK_NEGATIVE:
      return -rightval;
    case TK_DEREF:
      return paddr_read(rightval, 1);
    case TK_MUL:
      return leftval * rightval;
    case TK_DIV:
      return leftval / rightval;
    case TK_MOD:
      return leftval % rightval;
    case TK_ADD:
      return leftval + rightval;
    case TK_MINUS:
      return leftval - rightval;
    case TK_EQ:
      return leftval == rightval;
    case TK_NEQ:
      return leftval != rightval;
    case TK_AND:
      return leftval && rightval;
    default:
      panic("op code %d not implemented\n", tokens[spindex].type);
  }
}

word_t expr(char* e, bool* success) {
  if (!make_token(e)) {
    *success = false;
    return 0;
  }

  return eval(0, nr_token, success);
}

void test_expr() {
  char tests[70000];
  char input_path[100];
  input_path[0] = '\0';
  strcat(input_path, getenv("NEMU_HOME"));
  strcat(input_path, "/tools/gen-expr/input");
  FILE* fp = fopen(input_path, "r");
  Assert(fp != NULL, "Error when opening input for expr test!");
  uint32_t expect;
  int count = 0;
  while (fgets(tests, 70000, fp) != NULL) {
    count++;
    tests[strlen(tests) - 1] = 0;
    char* pattern = strtok(tests, " ");
    sscanf(pattern, "%u", &expect);
    char* exprs = tests + strlen(pattern) + 1;
    bool success;
    uint32_t actual = expr(exprs, &success);
    Assert(success,
           "error when testing expr with %d-th testcase %s: expected %u, "
           "actually failure",
           count, exprs, expect);
    Assert(actual == expect,
           "error when testing expr with %d-th testcase %s: expected %u, "
           "actually %u",
           count, exprs, expect, actual);
  }
  Log("Test expr Done\n");
}
