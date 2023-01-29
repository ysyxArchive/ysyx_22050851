#include <am.h>
#include <klib-macros.h>
#include <klib.h>
#include <stdarg.h>

#if !defined(__ISA_NATIVE__) || defined(__NATIVE_USE_KLIB__)

int num2str(char* out, int num) {
  char bufs[30];
  int outp = 0;
  int bufp = 0;
  if (num == 0) {
    out[outp++] = '0';
  } else {
    if (num < 0) {
      out[outp++] = '-';
      num = -num;
    }
    while (num != 0) {
      bufs[bufp++] = '0' + num % 10;
      num = num / 10;
    }
    bufp--;
    while (bufp >= 0) {
      out[outp++] = bufs[bufp--];
    }
  }
  out[outp] = 0;
  return outp;
}

int printf(const char* fmt, ...) {
  va_list ap;
  va_start(ap, fmt);
  size_t fmtp = 0;
  size_t cnt = 0;
  char bufs[30];
  while (fmt[fmtp]) {
    if (fmt[fmtp] != '%') {
      putch(fmt[fmtp++]);
    } else {
      fmtp++;
      switch (fmt[fmtp]) {
        case 'd':
          int d = va_arg(ap, int);
          num2str(bufs, d);
          for (size_t p = 0; bufs[p]; p++) {
            putch(bufs[p]);
          }
          break;
        case 's':
          char* s = va_arg(ap, char*);
          for (size_t p = 0; s[p]; p++) {
            putch(s[p]);
          }
          break;
        case 'c':
          char* c = va_arg(ap, char*);
          putch(*c);
          break;
        default:
          panic("unsupported format ");
      }
      fmtp++;
    }
  }
  va_end(ap);
  return cnt;
}

int vsprintf(char* out, const char* fmt, va_list ap) {
  panic("Not implemented");
}

int sprintf(char* out, const char* fmt, ...) {
  va_list ap;
  va_start(ap, fmt);
  int outp = 0;
  int fmtp = 0;
  int offset = 0;
  while (fmt[fmtp]) {
    if (fmt[fmtp] != '%') {
      out[outp++] = fmt[fmtp++];
    } else {
      fmtp++;
      switch (fmt[fmtp]) {
        case 'd':
          int d = va_arg(ap, int);
          offset = num2str(out + outp, d);
          outp += offset;
          break;
        case 's':
          char* s = va_arg(ap, char*);
          strcpy(out + outp, s);
          outp += strlen(out + outp);
          break;
        case 'c':
          char* c = va_arg(ap, char*);
          out[outp++] = (*c);
          break;
        default:
          panic("unsupported format");
      }
      fmtp++;
    }
  }
  va_end(ap);
  out[outp] = 0;
  return outp;
}

int snprintf(char* out, size_t n, const char* fmt, ...) {
  panic("Not implemented");
}

int vsnprintf(char* out, size_t n, const char* fmt, va_list ap) {
  panic("Not implemented");
}

#endif
