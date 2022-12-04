all:
	verilator --cc --exe --build --trace -j 0 -Wall csrc/main.cpp vsrc/top.v 
	

test:
	echo "123"

%:	csrc/%.cpp	vsrc/%.v
	verilator --cc --exe --build --trace -j 0 -Wall csrc/$@.cpp vsrc/$@.v 

sim:
	$(call git_commit, "sim RTL") # DO NOT REMOVE THIS LINE!!!
	./obj_dir/V$1

clean:
	rm -rf obj_dir

include ../Makefile
