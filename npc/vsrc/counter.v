module counter(
    input clk,
    input rst,
    output reg out);

    initial begin
        out <= 0;
    end



    always @(posedge clk)
        out <=rst? 0 : out + 1;
endmodule
