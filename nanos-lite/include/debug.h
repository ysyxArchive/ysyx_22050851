#ifndef __DEBUG_H__
#define __DEBUG_H__

#include <common.h>

#define ANSI_FMT(str, fmt) fmt str ANSI_NONE
#define ANSI_FG_RED "\33[1;31m"
#define Log(format, ...)                                                       \
  printf("\33[1;35m[%s,%d,%s] " format "\33[0m\n", __FILE__, __LINE__,         \
         __func__, ##__VA_ARGS__)

#define Panic(format, ...)                                                     \
  do {                                                                         \
    Log("\33[1;31msystem panic: " format, ##__VA_ARGS__);                      \
    halt(1);                                                                   \
  } while (0)

#define Assert(cond, format, ...)                                              \
  do {                                                                         \
    if (!(cond)) {                                                             \
      Log(format, ##__VA_ARGS__);                                              \
      assert(cond);                                                            \
    }                                                                          \
  } while (0)

#define TODO() panic("please implement me")

#endif
