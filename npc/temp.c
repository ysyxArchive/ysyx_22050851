#include <stdio.h>
#include <stdint.h>

int main() {
    int64_t a = 11, b = -222;
    __int128_t c = (__int128_t)a * b;
    printf("%x\n", c >> 64);
}