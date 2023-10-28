#ifndef __CONFIG_H__
#define __CONFIG_H__
// lightsss快照周期间隔
#define LIGHT_SSS_CYCLE_INTERVAL 100000
// 最大记录周期数量
#define WAVE_TRACE_CLOCKS 1000
// sdl事件处理间隔
#define DEVICE_UPDATE_INTERVAL 100000
// 最大pc变化间隔周期，超过会触发bad halt
#define MAX_WAIT_ROUND 300
#define MTRACE
// npc 内部debug
// #define ENABLE_DEBUG


#endif