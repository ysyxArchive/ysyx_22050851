# all build files
BUILD_DIR = ./build
# verilator build files
OBJ_DIR = $(BUILD_DIR)/obj_dir


export PATH := $(PATH):$(abspath ./utils)
test:
	mill -i __.test

verilog:
	$(call git_commit, "generate verilog")
	mkdir -p $(BUILD_DIR)
	mill -i __.test.runMain Elaborate -d $(BUILD_DIR)

help:
	mill -i __.test.runMain Elaborate --help

compile:
	mill -i __.compile

bsp:
	mill -i mill.bsp.BSP/install

reformat:
	mill -i __.reformat

checkformat:
	mill -i __.checkFormat

clean:
	-rm -rf $(BUILD_DIR)

build:
	@rm -rf $(OBJ_DIR)


sim:
	$(call git_commit, "sim RTL") # DO NOT REMOVE THIS LINE!!!
	@echo "Write this Makefile by yourself."

.PHONY: test verilog help compile bsp reformat checkformat clean sim build
include ../Makefile
