TOPNAME = CPU
# all build files
BUILD_DIR = ./build
# verilator build files
OBJ_DIR = $(BUILD_DIR)/obj_dir

VERILATOR = verilator
VERILATOR_CFLAGS += -MMD --build -cc  \
				-O3 --x-assign fast --x-initial fast --noassert --trace
BIN = $(BUILD_DIR)/$(TOPNAME)

VSRCS = $(shell find $(abspath ./build) -name "*.v")
CSRCS = $(shell find $(abspath ./csrc) -name "*.c" -or -name "*.cc" -or -name "*.cpp")

INCFLAGS = $(addprefix -I, $(INC_PATH))
CFLAGS += $(INCFLAGS) -DTOP_NAME="\"V$(TOPNAME)\""
LDFLAGS += -lSDL2 -lSDL2_image

export PATH := $(PATH):$(abspath ./utils)

default: $(BIN)

$(BIN): $(VSRCS) $(CSRCS)
	@rm -rf $(OBJ_DIR)
	$(VERILATOR) $(VERILATOR_CFLAGS) \
		--top-module $(TOPNAME) $^ \
		$(addprefix -CFLAGS , $(CFLAGS)) $(addprefix -LDFLAGS , $(LDFLAGS)) \
		--Mdir $(OBJ_DIR) --exe -o $(abspath $(BIN))


test:
	mill -i __.test

verilog:
	$(call git_commit, "generate verilog")
	mkdir -p $(BUILD_DIR)
	mill -i __.test.runMain Elaborate -td $(BUILD_DIR)

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
	


sim: $(BIN)
	$(call git_commit, "sim RTL") # DO NOT REMOVE THIS LINE!!!
	$(BIN)

.PHONY: test verilog help compile bsp reformat checkformat clean sim build
include ../Makefile
