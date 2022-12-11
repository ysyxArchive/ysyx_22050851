all:
	verilator --cc --exe --build --trace -j 0 -Wall csrc/main.cpp vsrc/top.v vsrc/counter.v
	

sim:
	$(call git_commit, "sim RTL") # DO NOT REMOVE THIS LINE!!!
	make all
	./obj_dir/Vtop

clean:
	rm -rf obj_dir

include ../Makefile
