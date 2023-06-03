
#include "device/map.h"
#include <common.h>
// copy the source to the ringbuf
void add_inst_to_ring(char *source);

// print ringbuf
void print_ring_buf();
void etrace(bool is_call, paddr_t source, word_t NO, paddr_t target);
void mtrace(bool is_read, paddr_t addr, int len, word_t data);
void init_ftrace(char *elflocation[], const int elfCount);
struct Decode;
void check_jump(struct Decode *s);
void show_position();
void dtrace(bool is_read, paddr_t addr, int len, word_t data, const IOMap *map);