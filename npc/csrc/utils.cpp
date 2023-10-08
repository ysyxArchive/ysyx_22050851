#include <common.h>
#include <difftest.h>
#include <getopt.h>
#include "VCPU.h"
#include "VCPU__Dpi.h"
#include "mem.h"
#include "tools/lightsss.h"
#include "verilated.h"
#include "verilated_dpi.h"
#include "verilated_vcd_c.h"

static char* diff_so_file;
static char* img_file;
static char* log_file;
static char* elf_file;

int parse_args(int argc, char* argv[]) {
  const struct option table[] = {
      // {"batch", no_argument, nullptr, 'b'},
      {"log", required_argument, nullptr, 'l'},
      {"elf", required_argument, nullptr, 'e'},
      {"diff", required_argument, nullptr, 'd'},
      // {"port", required_argument,nullptr, 'p'},
      {"help", no_argument, nullptr, 'h'},
      {0, 0, nullptr, 0},
  };
  int o;
  while ((o = getopt_long(argc, argv, /*"-bhl:d:p:e:"*/ "-l:", table,
                          nullptr)) != -1) {
    switch (o) {
      // case 'b':
      //   sdb_set_batch_mode();
      //   break;
      // case 'p':
      //   sscanf(optarg, "%d", &difftest_port);
      //   break;
      case 'l':
        log_file = optarg;
        break;
      case 'd':
        diff_so_file = optarg;
        break;
      case 'e':
        elf_file = optarg;
        break;
      case 1:
        img_file = optarg;
        break;
      default:
        printf("Usage: %s [OPTION...] IMAGE [args]\n\n", argv[0]);
        // printf("\t-b,--batch              run with batch mode\n");
        // printf("\t-l,--log=FILE           output log to FILE\n");
        printf("\t-e,--elf=FILE           .elf file to input\n");
        printf(
            "\t-d,--diff=REF_SO        run DiffTest with reference REF_SO\n");
        // printf("\t-p,--port=PORT          run DiffTest with port PORT\n");
        printf("\n");
        exit(0);
    }
  }
  return 0;
}

void load_files() {
  Assert(img_file, "img file not found!");
  Log("detected img file: %s", img_file);
  init_memory(img_file);

  Assert(diff_so_file, "difftest ref file not found!");
  Log("detected so file: %s", diff_so_file);
  load_difftest_so(diff_so_file);
}

VerilatedVcdC* tfp;
VCPU* top;
extern LightSSS lightSSS;

void init_vcd_trace() {
  VerilatedContext* contextp = new VerilatedContext;
  Verilated::traceEverOn(true);  // 导出vcd波形需要加此语句
  tfp = new VerilatedVcdC();     // 导出vcd波形需要加此语句
  top = new VCPU{contextp};
  top->reset = false;
  top->trace(tfp, 0);
  tfp->open("wave.vcd");  // 打开vcd
}

extern int npc_clock;
int tfp_clock = 0;
void eval_trace() {
  top->eval();
  if (lightSSS.is_not_good() &&
      lightSSS.get_end_cycles() - npc_clock < WAVE_TRACE_CLOCKS) {
    tfp->dump(tfp_clock++);
    tfp->flush();
    printf("%d\n", npc_clock);
  }
  npc_clock++;
}