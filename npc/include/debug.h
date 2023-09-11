
#ifndef __DEBUG_H__
#define __DEBUG_H__

#include "tools/lightsss.h"
#include <assert.h>
#include <util.h>
#define CONFIG_TARGET_AM true

extern int npc_clock;
extern LightSSS lightSSS;
extern bool is_bad_halt;
#define Log(format, ...)                                                       \
  _Log(ANSI_FMT("[%s:%d %s] " format, ANSI_FG_BLUE) "\n", __FILE__, __LINE__,  \
       __func__, ##__VA_ARGS__)

#define Assert(cond, format, ...)                                              \
  do {                                                                         \
    if (!(cond)) {                                                             \
      printf(ANSI_FMT(format, ANSI_FG_RED) "\n", ##__VA_ARGS__);               \
      if (!lightSSS.is_child()) {                                              \
        lightSSS.wakeup_child(npc_clock);                                      \
        isa_reg_display();                                                     \
      }                                                                        \
      is_bad_halt = true;                                                      \
    }                                                                          \
  } while (0)

#define panic(format, ...) Assert(0, format, ##__VA_ARGS__)

#define TODO() panic("please implement me")

#endif
