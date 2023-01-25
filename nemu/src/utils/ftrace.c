#include <common.h>
#include <cpu/decode.h>
#include <elf.h>
#include <malloc.h>
#include <stdio.h>
#include <string.h>
typedef struct FuncNode {
  uint64_t name_index;
  char name[30];
  uint64_t start;
  uint64_t length;
  struct FuncNode* next;
} FuncNode;
typedef struct PositionNode {
  char funcName[30];
  uint64_t position;
  uint64_t nextPosition;
  bool isret;
  struct PositionNode* next;
} PositionNode;

PositionNode positionNode = {.next = NULL};
PositionNode* positionTail = NULL;
int positionLength = 0;
FuncNode headFuncNode = {.next = NULL};

void init_ftrace(const char* elflocation) {
  if (!elflocation) {
    Log("ftrace is %s", ANSI_FMT("OFF", ANSI_FG_RED));
    return;
  }
  size_t ret;
  FILE* fp = fopen(elflocation, "r");
  FILE* fp2 = fopen(elflocation, "r");
  Assert(fp && fp2, "Cannot open elf file at %s", elflocation);
  Elf64_Ehdr elfHeader;
  ret = fread(&elfHeader, sizeof(elfHeader), 1, fp);
  Assert(ret > 0, "error when reading");

  fseek(fp, elfHeader.e_shoff, SEEK_SET);
  Elf64_Shdr section_header_buf;

  Elf64_Shdr section_header_symtab = {.sh_type = SHT_NULL},
             section_header_strtab = {.sh_type = SHT_NULL};

  printf("%d", elfHeader.e_shnum);
  for (int i = 0; i < elfHeader.e_shnum; i++) {
    ret = fread(&section_header_buf, sizeof(section_header_buf), 1, fp);
    Assert(ret > 0, "error when reading");
    if (section_header_buf.sh_type == SHT_SYMTAB) {
      section_header_symtab = section_header_buf;
    } else if (section_header_buf.sh_type == SHT_STRTAB &&
               section_header_strtab.sh_type == SHT_NULL) {
      section_header_strtab = section_header_buf;
    }
  }
  Assert(section_header_symtab.sh_type != SHT_NULL,
         "error not found symbol table");
  Assert(section_header_strtab.sh_type != SHT_NULL,
         "error not found string table");
  fseek(fp, section_header_symtab.sh_offset, SEEK_SET);
  Elf64_Sym symbuf;
  for (int i = 0;
       i < section_header_symtab.sh_size / section_header_symtab.sh_entsize;
       i++) {
    ret = fread(&symbuf, sizeof(symbuf), 1, fp);
    Assert(ret > 0, "error when reading");
    if (ELF64_ST_TYPE(symbuf.st_info) == STT_FUNC) {
      FuncNode* node = (FuncNode*)malloc(sizeof(FuncNode));
      node->start = symbuf.st_value;
      node->length = symbuf.st_size;
      node->name_index = symbuf.st_name;
      fseek(fp2, section_header_strtab.sh_offset + symbuf.st_name, SEEK_SET);
      ret = fread(node->name, sizeof(char), 30, fp2);
      Assert(ret > 0, "error when reading");
      node->name[29] = 0;
      node->next = headFuncNode.next;
      headFuncNode.next = node;
    }
  }
  FuncNode* p = headFuncNode.next;
  while (p) {
    printf("%s %lx %lx\n", p->name, p->start, p->length);
    p = p->next;
  }
  fclose(fp);
  fclose(fp2);
  return;
}

void getin(Decode* s) {
  uint64_t dnpc = s->dnpc;
  FuncNode* node = headFuncNode.next;
  PositionNode* target = (PositionNode*)malloc(sizeof(PositionNode));
  while (node && node->start > dnpc && node->start + node->length <= dnpc) {
    node = node->next;
  }
  target->position = s->pc;
  target->nextPosition = s->dnpc;
  target->isret = false;
  strcpy(target->funcName, node ? node->name : "???");
  target->next = NULL;
  positionTail->next = target;
  positionTail->next = positionTail;
  positionLength++;
}

void getout(Decode* s) {
  uint64_t pc = s->pc;
  FuncNode* node = headFuncNode.next;
  PositionNode* target = (PositionNode*)malloc(sizeof(PositionNode));
  while (node && node->start > pc && node->start + node->length <= pc) {
    node = node->next;
  }
  target->position = s->pc;
  target->nextPosition = s->dnpc;
  target->isret = true;
  strcpy(target->funcName, node ? node->name : "???");
  target->next = NULL;
  positionTail->next = target;
  positionTail->next = positionTail;
  positionLength++;
}

void check_jump(Decode* s) {
  if (strcmp("jal", s->isa.inst.instname) == 0) {
    getin(s);
  }
  if (strcmp("jalr", s->isa.inst.instname) == 0) {
    getout(s);
  }
  printf("%s", s->isa.inst.instname);
}

void show_position() {
printf("123");
  PositionNode* p = positionNode.next;
  while (p) {
    printf("%lx: %s [%s@0x%lx]\n", p->position, p->isret ? "ret" : "call",
           p->funcName, p->nextPosition);
        p = p->next;
  }
}