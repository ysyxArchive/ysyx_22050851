.section .text
.globl _start

_start:
    # 定义变量
    lui t0, 2   # 变量1
    lui t1, 4   # 变量2
    lui t2, 6   # 变量3
    lui t3, 8   # 变量4
    lui t4, 10  # 变量5
    lui t5, 12  # 变量6
    
    # 执行好多次加法运算
    add a0, t0, t1 
    add a0, a0, t2 
    add a0, a0, t3 
    add a0, a0, t4 
    add a0, a0, t5 
    add a0, a0, t0 
    lui a0, 0  # 归零
    
    # 程序退出
    ebreak
    