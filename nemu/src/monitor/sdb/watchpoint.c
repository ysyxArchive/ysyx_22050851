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

#define NR_WP 32
#define WP_EXPRESSION_MAX_LEN 100
typedef struct watchpoint {
  int NO;
  char expr[WP_EXPRESSION_MAX_LEN];
  struct watchpoint* next;

  /* TODO: Add more members if necessary */

} WP;

static WP wp_pool[NR_WP] = {};
static WP *head = NULL, *free_ = NULL;

void init_wp_pool() {
  int i;
  for (i = 0; i < NR_WP; i++) {
    wp_pool[i].NO = i;
    wp_pool[i].next = (i == NR_WP - 1 ? NULL : &wp_pool[i + 1]);
  }

  head = NULL;
  free_ = wp_pool;
}

/**
 * @return watchpoint id if success, -1 if error and print error info
 */
int new_wp(char* expr) {
  if (!free_) {
    printf("Can not new more watchpoints!\n");
    return -1;
  }
  if (strlen(expr) >= WP_EXPRESSION_MAX_LEN) {
    printf("expression too long!\n");
    return -1;
  }
  strcpy(free_->expr, expr);
  WP* p = free_->next;
  free_->next = head;
  head = free_;
  free_ = p;
  return head->NO;
}

bool free_wp(int NO) {
  if (!head) {
    printf("Watchpoint %d not found!", NO);
    return false;
  }
  if (head->NO == NO) {
    WP* p = head;
    head = head->next;
    p->next = free_;
    free_ = p;
    return true;
  }
  WP* p = head;
  while (p->next != NULL) {
    if (p->next->NO == NO) {
      WP* q = p->next;
      p->next = p->next->next;
      q->next = free_;
      free_ = q;
      return true;
    }
    p = p->next;
  }
  printf("Watchpoint %d not found!", NO);
  return false;
}

void list_wp() {
  WP* p = head;
  while (p != NULL) {
    printf("%d %s\n", p->NO, p->expr);
    p = p->next;
  }
}

char* get_wp_expr(int NO) {
  WP* p = head;
  while (p != NULL) {
    if (p->NO == NO) {
      return p->expr;
    }
    p = p->next;
  }
  printf("Watchpoint %d not found!", NO);
  return NULL;
}

/**
 * @return wp.NO if watchpoint triggered, else return -1;
 */
int check_watchpoint() {
  WP* p = head;
  while (p != NULL) {
    bool s;
    uint32_t res = expr(p->expr, &s);
    if (s && res) {
      return p->NO;
    }
    p = p->next;
  }
  return -1;
}

/* TODO: Implement the functionality of watchpoint */
