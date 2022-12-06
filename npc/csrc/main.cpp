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
  Vtop* top = new Vtop{contextp, "123"};
  top->trace(tfp, 0);
  tfp->open("wave.vcd");
  printf("evaling\b\n");
  bool clk = 0;
  while (i++ <= 20) {
    clk = !clk;
    top->clk = clk;
    top->in = 1;
    top->rst = 0;

    printf("clk = %d, out = %d\n", top->clk, top->out);
    tfp->dump(contextp->time());
    contextp->timeInc(1);
  }
  tfp->close();

  return 0;
}
