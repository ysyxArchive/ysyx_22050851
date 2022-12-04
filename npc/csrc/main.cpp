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
    int a = getrand(256);
    int s = getrand(3);
    top->a = a;
    top->s = s;
    top->eval();
    int y = top->y;
    printf("a = %d, s = %d, y = %d\n", a, s, y);

    tfp->dump(contextp->time());
    contextp->timeInc(1);
  }
  tfp->close();

  return 0;
}
