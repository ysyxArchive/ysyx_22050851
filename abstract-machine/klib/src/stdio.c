#include <am.h>
#include <klib-macros.h>
#include <klib.h>
#include <stdarg.h>

#if !defined(__ISA_NATIVE__) || defined(__NATIVE_USE_KLIB__)

char buffer_string[1000];

int num2str(char *out, uint64_t num, bool zero_padding, uint8_t width,
            int round, bool unsign) {
  char bufs[30];
  int outp = 0;
  int bufp = 0;
  bool isneg = false;
  if (num == 0) {
    bufs[bufp++] = '0';
  }
  if (!unsign && (int64_t)num < 0) {
    isneg = true;
    num = -(int64_t)num;
  }
  while (num != 0) {
    bufs[bufp++] = '0' + num % round;
    if (bufs[bufp - 1] > '9') {
      bufs[bufp - 1] += 'a' - '9' - 1;
    }
    num = num / round;
  }
  if (bufp < width) {
    while (bufp < width - 1) {
      bufs[bufp++] = zero_padding ? '0' : ' ';
    }
    bufs[bufp++] = isneg ? '-' : zero_padding ? '0' : ' ';
  } else if (isneg) {
    bufs[bufp++] = '-';
  }
  while (bufp > 0) {
    bufp--;
    out[outp++] = bufs[bufp];
  }
  out[outp] = 0;
  return outp;
}

int str2num(const char *str, int length) {
  int ret = 0;
  for (int i = 0; i < length; i++) {
    ret = ret * 10 + str[i] - '0';
  }
  return ret;
}

int check_indent(const char *str, uint64_t data, char **ret) {
  int p = 0;
  bool zero_padding;
  uint8_t width;
  while (1) {
    switch (str[p]) {
    case 'd':
    case 'x':
      zero_padding = str[0] == '0';
      width = str2num(str, p);
      num2str(buffer_string, data, zero_padding, width, str[p] == 'd' ? 10 : 16,
              false);
      *ret = buffer_string;
      return p + 1;
    case 'p':
      num2str(buffer_string, data, true, 16, 16, true);
      *ret = buffer_string;
      return p + 1;
    case 'f':
      // todo: makeit
      return 1;
    case 's':
      *ret = (char *)data;
      return p + 1;
    case 'c':
      buffer_string[0] = (char)data;
      buffer_string[1] = 0;
      *ret = buffer_string;
      return p + 1;
    default:
      if (p > 10) {
        panic("print indent not found or not supported!");
      }
    }
    p++;
  }
}

int printf(const char *fmt, ...) {
  va_list ap;
  va_start(ap, fmt);
  size_t fmtp = 0;
  size_t cnt = 0;
  while (fmt[fmtp]) {
    if (fmt[fmtp] != '%') {
      putch(fmt[fmtp++]);
    } else {
      fmtp++;
      char *rets;
      int64_t data = (int64_t)va_arg(ap, uint64_t);
      assert(data >= 0);
      fmtp += check_indent(fmt + fmtp, data, &rets);
      for (int i = 0; rets[i]; i++) {
        putch(rets[i]);
      }
    }
  }
  va_end(ap);
  return cnt;
}

int vsprintf(char *out, const char *fmt, va_list ap) {
  panic("Not implemented");
}

int sprintf(char *out, const char *fmt, ...) {
  va_list ap;
  va_start(ap, fmt);
  int outp = 0;
  int fmtp = 0;
  while (fmt[fmtp]) {
    if (fmt[fmtp] != '%') {
      out[outp++] = fmt[fmtp++];
    } else {
      fmtp++;
      char *rets;
      fmtp += check_indent(fmt + fmtp, va_arg(ap, uint64_t), &rets);
      for (int i = 0; rets[i]; i++) {
        out[outp++] = rets[i];
      }
    }
  }
  va_end(ap);
  out[outp] = 0;
  return outp;
}

int snprintf(char *out, size_t n, const char *fmt, ...) {
  panic("Not implemented");
}

int vsnprintf(char *out, size_t n, const char *fmt, va_list ap) {
  panic("Not implemented");
}

#endif
