module counter(
    input in,
    input clk,
    input rst,
    output reg out);

    reg cnt;

    initial begin
        cnt = 0;
        out = 0;
    end

    always @(posedge clk)
        cnt <= rst? 0 : cnt + in;
        out  <= in && cnt;
endmodule
