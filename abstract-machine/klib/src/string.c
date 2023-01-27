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
  size_t i = 0;
  size_t len = strlen(src);
  while (i < len) {
    dst[i] = src[i];
    i++;
  }
  dst[i] = 0;
  return dst;
}

char* strncpy(char* dst, const char* src, size_t n) {
  size_t len = strlen(src);
  for (size_t i = 0; i < n; i++) {
    strcpy(dst + i * len, src);
  }
  dst[n * len] = 0;
  return dst;
}

char* strcat(char* dst, const char* src) {
  size_t dstlen = strlen(dst);
  size_t srclen = strlen(src);
  strcpy(dst + dstlen, src);
  dst[dstlen + srclen] = 0;
  return dst;
}

int strcmp(const char* s1, const char* s2) {
  size_t i = 0;
  while (s1[i] && s2[i] && s1[i] == s2[i]) {
    i++;
  }
  return s1[i] - s2[i];
}

int strncmp(const char* s1, const char* s2, size_t n) {
  size_t i = 0;
  while (i < n && s1[i] && s2[i] && s1[i] == s2[i]) {
    i++;
  }
  if (i == n) {
    return 0;
  } else {
    return s1[i] - s2[i];
  }
}

void* memset(void* s, int c, size_t n) {
  uint8_t* p = s;
  for (size_t i = 0; i < n; i++) {
    p[i] = (uint8_t)c;
  }
  return s;
}

void* memmove(void* dst, const void* src, size_t n) {
  uint8_t* temp = (uint8_t*)malloc(n);
  memcpy(temp, src, n);
  memcpy(dst, temp, n);
  free(temp);
  return dst;
}

void* memcpy(void* out, const void* in, size_t n) {
  uint8_t* outp = out;
  const uint8_t* inp = in;
  for (size_t i = 0; i < n; i++) {
    outp[i] = inp[i];
  }
  return out;
}

int memcmp(const void* s1, const void* s2, size_t n) {
  const uint8_t* p1 = s1;
  const uint8_t* p2 = s2;
  uint8_t ret = 0;
  size_t i = 0;
  while (i < n && ret == 0) {
    ret = p1[i] - p2[i];
    i++;
  }
  return ret;
}

#endif
