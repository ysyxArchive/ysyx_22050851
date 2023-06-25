#include "fs.h"
#include <elf.h>
#include <proc.h>
#include <ramdisk.h>
#include <stdio.h>
#ifdef __LP64__
#define Elf_Ehdr Elf64_Ehdr
#define Elf_Phdr Elf64_Phdr
#else
#define Elf_Ehdr Elf32_Ehdr
#define Elf_Phdr Elf32_Phdr
#endif
#define ADDR_BEGIN 0x83000000
static uintptr_t loader(PCB *pcb, const char *filename) {
  int fd = fs_open(filename, 0, 0);
  fs_lseek(fd, 0, SEEK_SET);
  Elf_Ehdr elfHeader;
  fs_read(fd, &elfHeader, sizeof(elfHeader));
  Log("%x %x %x %x", elfHeader.e_ident[0], elfHeader.e_ident[1], elfHeader.e_ident[2], elfHeader.e_ident[3]);
  Assert(elfHeader.e_ident[0] == ELFMAG0 && elfHeader.e_ident[1] == ELFMAG1 &&
             elfHeader.e_ident[2] == ELFMAG2 && elfHeader.e_ident[3] == ELFMAG3,
         "error file %s not elf", filename);
  Assert(elfHeader.e_machine == EM_RISCV, "exec not support riscv");
  Elf_Phdr prog_header_buf;
  for (int i = 0; i < elfHeader.e_phnum; i++) {
    fs_lseek(fd, elfHeader.e_phoff + sizeof(prog_header_buf) * i, SEEK_SET);
    fs_read(fd, &prog_header_buf, sizeof(prog_header_buf));
    if (prog_header_buf.p_type != PT_LOAD) {
      continue;
    }
    fs_lseek(fd, prog_header_buf.p_offset, SEEK_SET);
    fs_read(fd, (uint8_t *)pf + (prog_header_buf.p_vaddr - (uint64_t)pf),
            prog_header_buf.p_filesz);

    memset((uint8_t *)pf + (prog_header_buf.p_filesz + prog_header_buf.p_vaddr -
                            (uint64_t)pf),
           0, prog_header_buf.p_memsz - prog_header_buf.p_filesz);
  }
  return elfHeader.e_entry;
}

void naive_uload(PCB *pcb, const char *filename) {
  uintptr_t entry = loader(pcb, filename);
  Log("Jump to entry = %p", (void *)entry);
  ((void (*)())entry)();
}
