#include <assert.h>
#include <stdio.h>
#include <stdlib.h>
#include <verilated.h>
#include <verilated_vcd_c.h>
#include "Vtop.h"

int getrand(int max) {
  return rand() % max;
}

int main(int argc, char** argv) {
  srand(time(0));
  int i = 0;
  VerilatedContext* contextp = new VerilatedContext;

  contextp->commandArgs(argc, argv);
  VerilatedVcdC* tfp = new VerilatedVcdC();
  contextp->traceEverOn(true);
  Vtop* top = new Vtop{contextp};
  top->trace(tfp, 0);
  tfp->open("wave.vcd");
  printf("evaling\b\n");
  while (i++ <= 20) {
    int in = 0;
    for (int i = 0; i < 6; i++) {
      in = (in << 4) + getrand(16);
    }
    top->cpudbgdata = in;
    top->eval();
    int out0 = top->HEX0;
    int out1 = top->HEX1;
    int out2 = top->HEX2;
    int out3 = top->HEX3;
    int out4 = top->HEX4;
    int out5 = top->HEX5;
    printf("a = %x, o0 = %d, o1 = %d, o2 = %d, o3 = %d, o4 = %d, o5 = %d\n", in,
           out0, out1, out2, out3, out4, out5);

    tfp->dump(contextp->time());
    contextp->timeInc(1);
  }
  tfp->close();

  return 0;
}
