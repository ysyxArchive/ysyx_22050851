override VERILATOR =  verilator 
IS_DEBUG = $(filter debug,$(MAKECMDGOALS))
<<<<<<< HEAD
=======
IS_HW = $(filter hardware,$(MAKECMDGOALS))
>>>>>>> adaab1e8590675071c22ec50f610816123747f3a

TOPNAME = CPU  

BUILD_DIR = ./build  # all build files dir

BIN = $(join $(BUILD_DIR), $(join /, $(if $(IS_DEBUG), $(join $(TOPNAME), _DEBUG), $(TOPNAME)))) 
# verilator build files
OBJ_DIR = $(if $(IS_DEBUG), $(join $(BUILD_DIR), /obj_dir_debug), $(join $(BUILD_DIR), /obj_dir)) 
<<<<<<< HEAD
VERILOG_DIR = $(if $(IS_DEBUG), $(join $(BUILD_DIR), /veri_debug), $(join $(BUILD_DIR), /veri)) 
=======
VERILOG_DIR = $(if $(IS_DEBUG), $(join $(BUILD_DIR), /veri_debug), $(if $(IS_HW), $(join $(BUILD_DIR), /veri_hw), $(join $(BUILD_DIR), /veri)))
>>>>>>> adaab1e8590675071c22ec50f610816123747f3a

VERILATOR_FLAGS += -MMD --build -cc -j 0 -O3 --x-assign fast --x-initial fast --noassert -Wno-WIDTH $(if $(IS_DEBUG), --trace --prof-cfuncs) 

# source files
VSRCS = $(sort $(shell find $(abspath $(VERILOG_DIR)) -name "*.v" -or -name "*.sv") $(abspath $(join $(VERILOG_DIR), /CPU.sv)))
CSRCS = $(shell find $(abspath ./csrc) -name "*.c" -or -name "*.cc" -or -name "*.cpp")
INCLUDES = $(shell find $(abspath ./include) -name "*.h")
GEN_SCALA = $(abspath ./playground/src/decode/InstContorlDecoder.scala)
SCALAS = $(shell find $(abspath ./playground/src) -name "*.scala") $(GEN_SCALA)
SCALATESTS = $(shell find $(abspath ./playground/src) -name "*.scala")

INC_PATH = $(abspath ./include)
INCFLAGS = $(addprefix -I, $(INC_PATH))

CFLAGS += $(INCFLAGS) -O3 -DTOP_NAME="\"V$(TOPNAME)\"" $(if $(IS_DEBUG), -DDEBUG,)
LDFLAGS += -lSDL2 -lSDL2_image -ldl
override ARGS += $(if $(IS_DEBUG), --diff=${NEMU_HOME}/build/riscv64-nemu-interpreter-so)
<<<<<<< HEAD
MILL_FLAGS = -td $(VERILOG_DIR) 
=======
MILL_FLAGS = -td $(VERILOG_DIR) $(if $(IS_HW), --rm-dpic)
>>>>>>> adaab1e8590675071c22ec50f610816123747f3a
export PATH := $(PATH):$(if $(IS_DEBUG),$(abspath ./tools/firtool_debug),$(abspath ./tools/firtool))


$(shell mkdir -p $(BUILD_DIR))

default: $(BIN)

$(BIN): $(INCLUDES) $(CSRCS) $(VSRCS)
	$(VERILATOR) $(VERILATOR_FLAGS) \
		--top-module $(TOPNAME) $(CSRCS) -sv $(VSRCS) \
		$(addprefix -CFLAGS , $(CFLAGS))   \
		$(addprefix -LDFLAGS , $(LDFLAGS)) \
		--Mdir $(OBJ_DIR) --exe -o $(abspath $(BIN))

$(VSRCS): $(SCALAS)
	$(call git_commit, "generate verilog")
	mill -i __.test.runMain Elaborate --split-verilog --warnings-as-errors $(MILL_FLAGS) 
	touch $(VSRCS)

$(GEN_SCALA): playground/scripts/codemap.csv playground/scripts/parsedecode.py
	python playground/scripts/parsedecode.py

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
<<<<<<< HEAD
	
.PHONY: test verilog help compile bsp reformat checkformat clean build gdb debug
=======

hardware: $(VSRCS)
	mkdir v_hw/
	sv2v -w v_hw/ $(VSRCS)

.PHONY: test verilog help compile bsp reformat checkformat clean build gdb debug hardware
>>>>>>> adaab1e8590675071c22ec50f610816123747f3a
include ../Makefile
