#include "fs.h"
#include "proc.h"
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
uintptr_t loader(PCB *pcb, const char *filename) {
  protect(&(pcb->as));
  Log("new page dir %p", pcb->as.ptr);
  int fd = fs_open(filename, 0, 0);
  fs_lseek(fd, 0, SEEK_SET);
  Elf_Ehdr elfHeader;
  fs_read(fd, &elfHeader, sizeof(elfHeader));
  Assert(elfHeader.e_ident[0] == ELFMAG0 && elfHeader.e_ident[1] == ELFMAG1 &&
             elfHeader.e_ident[2] == ELFMAG2 && elfHeader.e_ident[3] == ELFMAG3,
         "error file %s not elf", filename);
  Assert(elfHeader.e_machine == EM_RISCV, "exec not support riscv");
  Elf_Phdr prog_header_buf;
  // find address space
  uint64_t min_addr = (uint64_t)-1;
  uint64_t max_addr = 0;
  for (int i = 0; i < elfHeader.e_phnum; i++) {
    fs_lseek(fd, elfHeader.e_phoff + sizeof(prog_header_buf) * i, SEEK_SET);
    fs_read(fd, &prog_header_buf, sizeof(prog_header_buf));
    if (prog_header_buf.p_type != PT_LOAD) {
      continue;
    }
    min_addr =
        prog_header_buf.p_vaddr < min_addr ? prog_header_buf.p_vaddr : min_addr;
    max_addr = prog_header_buf.p_vaddr + prog_header_buf.p_memsz > max_addr
                   ? prog_header_buf.p_vaddr + prog_header_buf.p_memsz
                   : max_addr;
  }
  // determine minmax
  min_addr = min_addr - min_addr % PGSIZE;
  max_addr =
      max_addr + ((max_addr % PGSIZE) ? (PGSIZE - max_addr % PGSIZE) : 0);
  pcb->max_brk = max_addr;
  // alloc pages
  // int pages_need = (max_addr - min_addr) / PGSIZE;
  // uint8_t *pages =
  //     (uint8_t *)((uint64_t)new_page(pages_need) - PGSIZE * pages_need);
  // Log("alloc pages for addr from %x to %x", (uint32_t)min_addr,
  //     (uint32_t)max_addr);
  // for (int i = 0; i < pages_need; i++) {
  //   map(&(pcb->as), (void *)(min_addr + i * PGSIZE), pages + i * PGSIZE, 1);
  // }
  // read data
  for (int i = 0; i < elfHeader.e_phnum; i++) {
    fs_lseek(fd, elfHeader.e_phoff + sizeof(prog_header_buf) * i, SEEK_SET);
    fs_read(fd, &prog_header_buf, sizeof(prog_header_buf));
    if (prog_header_buf.p_type != PT_LOAD) {
      continue;
    }
    fs_lseek(fd, prog_header_buf.p_offset, SEEK_SET);
    printf("%x\n", (unsigned)prog_header_buf.p_vaddr);
    fs_read(fd, (uint8_t *)prog_header_buf.p_vaddr, prog_header_buf.p_filesz);
    memset((uint8_t *)(prog_header_buf.p_filesz + prog_header_buf.p_vaddr), 0,
           prog_header_buf.p_memsz - prog_header_buf.p_filesz);
  }
  return elfHeader.e_entry;
}

void naive_uload(PCB *pcb, const char *filename) {
  reset_fs();
  uintptr_t entry = loader(pcb, filename);
  Log("Jump to entry = %p", (void *)entry);
  ((void (*)())entry)();
}
