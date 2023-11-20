
#ifndef __DEBUG_H__
#define __DEBUG_H__

#include <assert.h>
#include <util.h>

#include "tools/lightsss.h"
#define CONFIG_TARGET_AM true

extern int npc_clock;
#ifdef DEBUG
extern LightSSS lightSSS;
#define WAKE_CHILD()                  \
  if (!lightSSS.is_child()) {         \
    lightSSS.wakeup_child(npc_clock); \
    isa_reg_display();                \
  }
#else
#define WAKE_CHILD() ;
#endif
extern bool is_bad_halt, is_halt;
#define Log(format, ...)                                                      \
  _Log(ANSI_FMT("[%s:%d %s] " format, ANSI_FG_BLUE) "\n", __FILE__, __LINE__, \
       __func__, ##__VA_ARGS__)

#define Assert(cond, format, ...)                                \
  do {                                                           \
    if (!(cond)) {                                               \
      printf(ANSI_FMT(format, ANSI_FG_RED) "\n", ##__VA_ARGS__); \
      WAKE_CHILD()                                               \
      is_halt = true;                                            \
      is_bad_halt = true;                                        \
    }                                                            \
  } while (0)

#define panic(format, ...) Assert(0, format, ##__VA_ARGS__)

#define TODO() panic("please implement me")

void printCacheRate();

#endif
