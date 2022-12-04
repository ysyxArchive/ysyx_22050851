all:
	@echo "Write this Makefile by your self."
	verilator --cc --exe --build --trace -j 0 -Wall csrc/main.cpp vsrc/top.v 
	

sim:
	$(call git_commit, "sim RTL") # DO NOT REMOVE THIS LINE!!!
	./obj_dir/Vtop
	@echo "Write this Makefile by your self."

include ../Makefile
