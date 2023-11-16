override VERILATOR =  verilator 
IS_DEBUG = $(filter debug,$(MAKECMDGOALS))
TOPNAME = $(if $(IS_DEBUG), CPU_DEBUG, CPU)  

BUILD_DIR = ./build  # all build files dir

BIN = $(join $(BUILD_DIR), $(join /, $(TOPNAME))) 


# verilator build files
OBJ_DIR = $(if $(IS_DEBUG), $(addsuffix, $(BUILD_DIR), /obj_dir_debug), $(addsuffix, $(BUILD_DIR), /obj_dir_debug)) 

VERILATOR_FLAGS += -MMD --build -cc -j 0 -O3 --x-assign fast --x-initial fast --noassert -Wno-WIDTH $(if $(IS_DEBUG), --trace) 

# source files
VSRCS = $(shell find $(abspath ./build) -name "*.v" -or -name "*.sv")
CSRCS = $(shell find $(abspath ./csrc) -name "*.c" -or -name "*.cc" -or -name "*.cpp")
INCLUDES = $(shell find $(abspath ./include) -name "*.h")
GEN_SCALA = $(abspath ./playground/src/decode/InstContorlDecoder.scala)
SCALAS = $(shell find $(abspath ./playground/src) -name "*.scala") $(GEN_SCALA)
SCALATESTS = $(shell find $(abspath ./playground/src) -name "*.scala")

INC_PATH = $(abspath ./include)
INCFLAGS = $(addprefix -I, $(INC_PATH))

CFLAGS += $(INCFLAGS) -DTOP_NAME="\"V$(TOPNAME)\"" $(if $(IS_DEBUG), -DDEBUG)   
LDFLAGS += -lSDL2 -lSDL2_image -ldl
ARGS += $(if $(IS_DEBUG), --diff=${NEMU_HOME}/build/riscv64-nemu-interpreter-so)
MILL_FLAGS = -td $(BUILD_DIR) $(if $(IS_DEBUG), --debug)
export PATH := $(PATH):$(abspath ./tools)

default: $(BIN)

$(BIN): $(INCLUDES) $(CSRCS) $(VSRCS)
	$(VERILATOR) $(VERILATOR_FLAGS) \
		--top-module $(TOPNAME) $(CSRCS) -sv $(VSRCS) \
		$(addprefix -CFLAGS , $(CFLAGS))   \
		$(addprefix -LDFLAGS , $(LDFLAGS)) \
		--Mdir $(OBJ_DIR) --exe -o $(abspath $(BIN))

$(VSRCS): $(SCALAS)
	make verilog

$(GEN_SCALA): playground/scripts/codemap.csv playground/scripts/parsedecode.py
	python playground/scripts/parsedecode.py

verilog: $(SCALAS)
	$(call git_commit, "generate verilog")
	mkdir -p $(BUILD_DIR)
	mill -i __.test.runMain Elaborate --split-verilog --warnings-as-errors $(MILL_FLAGS)

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

debug: $(BIN)
	$(call git_commit, "sim RTL") # DO NOT REMOVE THIS LINE!!!
	$(BIN) $(IMG) $(ARGS) 

test: $(SCALATESTS)
	mill -i __.test

gdb: $(BIN)
	gdb -s $(BIN) --args $(BIN) $(IMG) $(ARGS) h 
	
.PHONY: test verilog help compile bsp reformat checkformat clean build gdb debug
include ../Makefile
