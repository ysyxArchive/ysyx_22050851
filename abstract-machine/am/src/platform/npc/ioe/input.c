#include <am.h>
#include <nemu.h>
#include <stdio.h>
#define KEYDOWN_MASK 0xFF

void __am_input_keybrd(AM_INPUT_KEYBRD_T* kbd) {
  uint32_t a = inl(KBD_ADDR);
  kbd->keydown = a >> 8;
  kbd->keycode = a & 0xFF;
}
