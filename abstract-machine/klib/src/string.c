#include <klib-macros.h>
#include <klib.h>
#include <stdint.h>

#if !defined(__ISA_NATIVE__) || defined(__NATIVE_USE_KLIB__)

size_t strlen(const char* s) {
  size_t i = 0;
  while (s[i]) {
    i++;
  }
  return i;
}

char* strcpy(char* dst, const char* src) {
  int i = 0;
  while (src[i]) {
    dst[i] = src[i];
    i++;
  }
  dst[i] = src[i];
  return dst;
}

char* strncpy(char* dst, const char* src, size_t n) {
  panic("Not implemented");
}

char* strcat(char* dst, const char* src) {
  int i = 0;
  while (dst[i])
    i++;
  int j = 0;
  while (src[j]) {
    dst[i + j] = src[j];
    j++;
  }
  dst[i + j] = 0;
  return dst;
}

int strcmp(const char* s1, const char* s2) {
  int i = 0;
  while (s1[i] && s2[i] && s1[i] == s2[i]) {
    i++;
  }
  return s1[i] - s2[i];
}

int strncmp(const char* s1, const char* s2, size_t n) {
  panic("Not implemented");
}

void* memset(void* s, int c, size_t n) {
  char* p = s;
  for (int i = 0; i < n; i++) {
    p[i] = (char)c;
  }
  return s;
}

void* memmove(void* dst, const void* src, size_t n) {
  panic("Not implemented");
}

void* memcpy(void* out, const void* in, size_t n) {
  panic("Not implemented");
}

int memcmp(const void* s1, const void* s2, size_t n) {
  const char* p1 = s1;
  const char* p2 = s2;
  char ret = 0;
  int i = 0;
  while (i < n && ret == 0) {
    ret = p1[i] - p2[i];
    i++;
  }
  return ret;
}

#endif
