AM_SRCS := platform/npc/trm.c 
#            platform/npc/ioe/ioe.c \
#            platform/npc/ioe/timer.c \
#            platform/npc/ioe/input.c \
#            platform/npc/ioe/gpu.c \
#            platform/npc/ioe/audio.c \
#            platform/npc/ioe/disk.c \
#            platform/npc/mpe.c

CFLAGS    += -fdata-sections -ffunction-sections
LDFLAGS   += -T $(AM_HOME)/scripts/linker.ld \
             --defsym=_pmem_start=0x80000000 --defsym=_entry_offset=0x0
LDFLAGS   += --gc-sections -e _start
NEMUFLAGS += -l $(shell dirname $(IMAGE).elf)/nemu-log.txt
NEMUFLAGS += --elf=$(IMAGE).elf

CFLAGS += -DMAINARGS=\"$(mainargs)\"
CFLAGS += -I$(AM_HOME)/am/src/platform/nemu/include
.PHONY: $(AM_HOME)/am/src/platform/nemu/trm.c

image: $(IMAGE).elf
	@$(OBJDUMP) -d $(IMAGE).elf > $(IMAGE).txt
	@echo + OBJCOPY "->" $(IMAGE_REL).bin
	@$(OBJCOPY) -S --set-section-flags .bss=alloc,contents -O binary $(IMAGE).elf $(IMAGE).bin


run: image
	echo 123
	$(MAKE) -C $(NPC_HOME) run ARGS="$(NEMUFLAGS)" IMG=$(IMAGE).bin

batch: image
	$(MAKE) -C $(NPC_HOME) ISA=$(ISA) batch ARGS="$(NEMUFLAGS)" IMG=$(IMAGE).bin

gdb: image
	$(MAKE) -C $(NPC_HOME) ISA=$(ISA) gdb ARGS="$(NEMUFLAGS)" IMG=$(IMAGE).bin
