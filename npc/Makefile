TOPNAME = CPU
# all build files
BUILD_DIR = ./build
# verilator build files
OBJ_DIR = $(BUILD_DIR)/obj_dir
OBJ_DIR_DEBUG = $(BUILD_DIR)/obj_dir_debug

override VERILATOR =  verilator 
VERILATOR_FLAGS += -MMD --build -cc -j 0 \
				-O3 --x-assign fast --x-initial fast --noassert 
VERILATOR_FLAGS += -Wno-WIDTH # 记得删掉
VERILATOR_DEBUG_FLAGS = $(VERILATOR_FLAGS) --trace
BIN = $(BUILD_DIR)/$(TOPNAME)
BIN_DEBUG = $(BUILD_DIR)/$(TOPNAME)_debug
GEN_SCALA = $(abspath ./playground/src/decode/InstContorlDecoder.scala)
VSRCS = $(shell find $(abspath ./build) -name "*.v" -or -name "*.sv")
CSRCS = $(shell find $(abspath ./csrc) -name "*.c" -or -name "*.cc" -or -name "*.cpp")
INCLUDES = $(shell find $(abspath ./include) -name "*.h")
SCALAS = $(shell find $(abspath ./playground/src) -name "*.scala") $(GEN_SCALA)
SCALATESTS = $(shell find $(abspath ./playground/src) -name "*.scala")
INC_PATH = $(abspath ./include)
INCFLAGS = $(addprefix -I, $(INC_PATH))
CFLAGS += $(INCFLAGS) -DTOP_NAME="\"V$(TOPNAME)\"" -DDEBUG 
LDFLAGS += -lSDL2 -lSDL2_image -ldl
ARGS_DEBUG = $(ARGS) --diff=${NEMU_HOME}/build/riscv64-nemu-interpreter-so
export PATH := $(PATH):$(abspath ./tools)

default: $(BIN)

$(BIN): $(INCLUDES) $(CSRCS) $(VSRCS)
	$(VERILATOR) $(VERILATOR_FLAGS) \
		--top-module $(TOPNAME) $(CSRCS) -sv $(VSRCS) \
		$(addprefix -CFLAGS , $(CFLAGS))   \
		$(addprefix -LDFLAGS , $(LDFLAGS)) \
		--Mdir $(OBJ_DIR) --exe -o $(abspath $(BIN))

$(BIN_DEBUG): $(INCLUDES) $(CSRCS) $(VSRCS)
	$(VERILATOR) $(VERILATOR_DEBUG_'FLAGS) \
		--top-module $(TOPNAME) $(CSRCS) -sv $(VSRCS)\
		$(addprefix -CFLAGS , $(CFLAGS))   \
		$(addprefix -LDFLAGS , $(LDFLAGS)) \
		--Mdir $(OBJ_DIR_DEBUG) --exe -o $(abspath $(BIN_DEBUG))


$(VSRCS): $(SCALAS)
	make verilog
$(GEN_SCALA): playground/scripts/codemap.csv playground/scripts/parsedecode.py
	python playground/scripts/parsedecode.py

verilog: $(SCALAS)
	$(call git_commit, "generate verilog")
	mkdir -p $(BUILD_DIR)
	mill -i __.test.runMain Elaborate -td $(BUILD_DIR) --split-verilog --warnings-as-errors

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

build: $(BIN)
	@rm -rf $(OBJ_DIR)
	


run: $(BIN)
	$(call git_commit, "sim RTL") # DO NOT REMOVE THIS LINE!!!
	$(BIN) $(IMG) $(ARGS) 

debug: $(BIN_DEBUG)
	$(call git_commit, "sim RTL") # DO NOT REMOVE THIS LINE!!!
	$(BIN_DEBUG) $(IMG) $(ARGS_DEBUG) 


test: $(SCALATESTS)
	mill -i __.test

gdb: $(BIN)
	gdb -s $(BIN) --args $(BIN) $(IMG) $(ARGS) h 
	
.PHONY: test verilog help compile bsp reformat checkformat clean build gdb debug
include ../Makefile
