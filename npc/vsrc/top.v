module top(
  input a[3:0],
  input s[1:0],
  output reg y);
  always @ (s or a)
    case (s)
        0: y = a[0];
        1: y = a[1];
        2: y = a[2];
        3: y = a[3];
endmodule
