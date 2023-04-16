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

static uintptr_t loader(PCB *pcb, const char *filename) {
  Elf_Ehdr elfHeader;
  ramdisk_read(&elfHeader, 0, sizeof(elfHeader));
  Assert(elfHeader.e_ident[0] == ELFMAG0 && elfHeader.e_ident[1] == ELFMAG1 &&
             elfHeader.e_ident[2] == ELFMAG2 && elfHeader.e_ident[3] == ELFMAG3,
         "error file not elf");

  Elf_Phdr prog_header_buf;
  uint64_t entryAddr = 0;
  for (int i = 0; i < elfHeader.e_phnum; i++) {
    ramdisk_read(&prog_header_buf, elfHeader.e_phoff, sizeof(prog_header_buf));
    if (prog_header_buf.p_type != PT_LOAD) {
      continue;
    }
    if (i == 0) {
      entryAddr = prog_header_buf.p_offset;
    }
    ramdisk_read((uint8_t *)pf + prog_header_buf.p_offset - (uint64_t)pf,
                 prog_header_buf.p_offset, prog_header_buf.p_filesz);
    memset((uint8_t *)pf + prog_header_buf.p_offset + prog_header_buf.p_filesz -
               (uint64_t)pf,
           0, prog_header_buf.p_memsz - prog_header_buf.p_filesz);
  }
  return entryAddr;
}

void naive_uload(PCB *pcb, const char *filename) {
  uintptr_t entry = loader(pcb, filename);
  Log("Jump to entry = %x", (uint64_t)entry);
  ((void (*)())entry)();
}
