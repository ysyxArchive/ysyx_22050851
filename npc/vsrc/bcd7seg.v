
module bcd7seg(
  input [3:0] in,
  output reg [6:0]out);
  always @ (in)
    case (in)
      0: out = 7'h3f;
      1: out = 7'h06;
      2: out = 7'h5b;
      3: out = 7'h4f;
      4: out = 7'h66;
      5: out = 7'h6d;
      6: out = 7'h7d;
      7: out = 7'h07;
      8: out = 7'h7f;
      9: out = 7'h6f;
      10: out = 7'h77;
      11: out = 7'h7c;
      12: out = 7'h39;
      13: out = 7'h5e;
      14: out = 7'h79;
      15: out = 7'h71;
    endcase
endmodule
