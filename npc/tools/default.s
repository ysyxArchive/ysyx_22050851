.section .text
.globl _start

_start:
    # 定义变量
    li t0, 2   # 变量1
    li t1, 4   # 变量2
    li t2, 6   # 变量3
    li t3, 8   # 变量4
    li t4, 10  # 变量5
    li t5, 12  # 变量6
    
    # 执行六次加法运算
    add a0, t0, t1  # 加法1: a0 = t0 + t1
    add a1, t1, t2  # 加法2: a1 = t1 + t2
    add a2, t2, t3  # 加法3: a2 = t2 + t3
    add a3, t3, t4  # 加法4: a3 = t3 + t4
    add a4, t4, t5  # 加法5: a4 = t4 + t5
    add a5, t5, t0  # 加法6: a5 = t5 + t0
    
    # 程序退出
    li a7, 10
    ecall
    