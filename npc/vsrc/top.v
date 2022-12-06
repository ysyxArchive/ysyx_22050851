
module top(
  input clk,
  input rst,
  output [3:0] out);

counter mycounter1(.rst(rst), .clk(clk), .out(out[3]));
counter mycounter2(.rst(rst), .clk(out[3]), .out(out[2]));
counter mycounter3(.rst(rst), .clk(out[2]), .out(out[1]));
counter mycounter4(.rst(rst), .clk(out[1]), .out(out[0]));
endmodule

