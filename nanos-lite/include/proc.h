#ifndef __PROC_H__
#define __PROC_H__

#include "common.h"
#include "memory.h"
#include "am.h"
#define STACK_SIZE (8 * PGSIZE)

typedef struct 
{
  uint8_t stack[STACK_SIZE] PG_ALIGN;
  struct
  {
    Context *cp;
    AddrSpace as;
    // we do not free memory, so use `max_brk' to determine when to call _map()
    uintptr_t max_brk;
  };
} PCB;

extern PCB *current;
void context_uload(PCB *pcb, const char *filename, char *const argv[], char *const envp[]);
Context *schedule(Context *prev);
void replacePCB(PCB* newone);
PCB* getPCB();
#endif
