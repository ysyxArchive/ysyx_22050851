#include <assert.h>
#include <stdio.h>
#include <stdlib.h>
#include <verilated.h>
#include <verilated_vcd_c.h>
#include "Vtop.h"
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
    int a = rand() & 1;
    int b = rand() & 1;
    int s = rand() & 1;
    top->a = a;
    top->b = b;
    top->s = s;
    top->eval();
    int y = top->y;
    printf("a = %d, b = %d, s = %d, y = %d\n", a, b, s, y);

    tfp->dump(contextp->time());
    contextp->timeInc(1);

  }
  tfp->close();

  return 0;
}
