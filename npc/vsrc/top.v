module top(
  input a,
  input b,
  input s,
  output y);
  assign y = (a & ~s) | (b & s);
endmodule
