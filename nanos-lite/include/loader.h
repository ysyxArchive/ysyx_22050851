#ifndef __LOADER_H__
#define __LOADER_H__
#include <proc.h>

void naive_uload(PCB *pcb, const char *filename);
uintptr_t loader(PCB *pcb, const char *filename);
#endif