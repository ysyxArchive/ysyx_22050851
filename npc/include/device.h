#ifndef __DIVECE_H__
#define __DIVECE_H__
#include "common.h"

#define VGA_WIDTH 400
#define VGA_HEIGHT 300

void init_device();

uint64_t gettime();

void update_vga();

void init_keyboard();
uint32_t get_key();
void send_key(uint8_t scancode, bool is_keydown);
#endif