
module top(
  input clk,
  input rst,
  output [3:0] out);

counter mycounter1(.rst(rst), .in(1), .clk(clk), .out(out[3]));
counter mycounter2(.rst(rst), .in(out[3]), .clk(clk), .out(out[2]));
counter mycounter3(.rst(rst), .in(out[2]), .clk(clk), .out(out[1]));
counter mycounter4(.rst(rst), .in(out[1]), .clk(clk), .out(out[0]));
endmodule

