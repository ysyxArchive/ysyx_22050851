.section .text
.globl _start

_start:
    # 定义两个64位整数
    lui a0, %hi(0x123acdf0)
    addi a0, a0, %lo(0x12345670)
    lui a1, %hi(0x9876dcba)
    addi a1, a1, %lo(0x980fdcba)

    # 调用mulh指令计算乘法高位
    mulh a2, a0, a1
    mulhsu a3, a0, a1
    mulhu a4, a0, a1
    # 将结果存储在寄存器a3中，用于输出
    mv a3, a2

    # 退出程序
    li a7, 10
    ebreak
