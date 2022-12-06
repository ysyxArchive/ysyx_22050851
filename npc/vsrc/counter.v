module counter(
    input clk,
    input rst,
    output reg out);

    initial begin
        out = 0;
    end

    always @(posedge rst)
        out = 0;


    always @(posedge clk)
        out <= out + 1;
endmodule
