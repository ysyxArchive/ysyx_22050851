import chisel3._


class MyModule extends Module {
    val io = IO(new Bundle {
        val in = Input(UInt(16.W))
        val out = Output(UInt(16.W))
    })

    io.out := RegNext(io.in)
}