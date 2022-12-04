#include <assert.h>
#include <stdio.h>
#include <stdlib.h>
#include <verilated.h>
#include <verilated_vcd_c.h>
#include "Vmain.h"
int main(int argc, char** argv) {
  int i = 0;
  VerilatedContext* contextp = new VerilatedContext;

  contextp->commandArgs(argc, argv);
  VerilatedVcdC* tfp = new VerilatedVcdC();
  contextp->traceEverOn(true);
  Vmain* top = new Vmain{contextp};
  top->trace(tfp, 0);
  tfp->open("wave.vcd");
  printf("evaling\b\n");
  while (i++ <= 20) {
    int a = rand() & 1;
    int b = rand() & 1;
    top->a = a;
    top->b = b;
    top->eval();

    printf("a = %d, b = %d, f = %d\n", a, b, top->c);

    tfp->dump(contextp->time());
    contextp->timeInc(1);

    assert(top->c == (a ^ b));
  }
  tfp->close();

  return 0;
}
