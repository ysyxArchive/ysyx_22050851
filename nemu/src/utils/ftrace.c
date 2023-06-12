#include <common.h>
#include <cpu/decode.h>
#include <elf.h>
#include <malloc.h>
#include <stdio.h>
#include <string.h>
#define FRACE_MAX_TRACE 50

typedef struct FuncNode {
  uint64_t name_index;
  char name[30];
  uint64_t start;
  uint64_t length;
  struct FuncNode *next;
} FuncNode;
typedef struct PositionNode {
  char funcName[30];
  uint64_t position;
  uint64_t nextPosition;
  bool isret;
  struct PositionNode *next;
} PositionNode;

PositionNode positionNode = {.next = NULL};
PositionNode *positionTail = &positionNode;
int positionLength = 0;
FuncNode headFuncNode = {.next = NULL};

void init_ftrace(char *elflocation[], const int elfCount) {
  for (int i = 0; i < elfCount; i++) {
    size_t ret;
    FILE *fp = fopen(elflocation[i], "r");
    FILE *fp2 = fopen(elflocation[i], "r");
    Assert(fp && fp2, "Cannot open elf file at %s", elflocation[i]);
    Elf64_Ehdr elfHeader;
    ret = fread(&elfHeader, sizeof(elfHeader), 1, fp);
    Assert(ret > 0, "error when reading header of %s", elflocation[i]);

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
        FuncNode *node = (FuncNode *)malloc(sizeof(FuncNode));
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
    fclose(fp);
    fclose(fp2);
  }
  return;
}

void prune() {
  if (positionLength > FRACE_MAX_TRACE) {
    PositionNode *p = positionNode.next;
    while (p && p->next && p->next->next && p->next->next->next) {
      if (!p->next->isret && p->next->next->isret) {
        PositionNode *q = p->next;
        p->next = p->next->next->next;
        free(q->next);
        free(q);
        positionLength -= 2;
        break;
      }
      p = p->next;
    }
  }
}

void getin(Decode *s) {
  uint64_t dnpc = s->dnpc;
  FuncNode *node = headFuncNode.next;
  PositionNode *target = (PositionNode *)malloc(sizeof(PositionNode));
  while (node && (node->start > dnpc || node->start + node->length <= dnpc)) {
    node = node->next;
  }
  target->position = s->pc;
  target->nextPosition = s->dnpc;
  target->isret = false;
  strcpy(target->funcName, node ? node->name : "???");
  target->next = NULL;
  positionTail->next = target;
  positionTail = positionTail->next;
  positionLength++;
  prune();
}

void getout(Decode *s) {
  uint64_t pc = s->pc;
  FuncNode *node = headFuncNode.next;
  PositionNode *target = (PositionNode *)malloc(sizeof(PositionNode));
  while (node && (node->start > pc || node->start + node->length <= pc)) {
    node = node->next;
  }
  target->position = s->pc;
  target->nextPosition = s->dnpc;
  target->isret = true;
  strcpy(target->funcName, node ? node->name : "???");
  target->next = NULL;
  positionTail->next = target;
  positionTail = positionTail->next;
  positionLength++;
  prune();
}

void check_jump(Decode *s) {
  // is call
  if ((s->isa.inst.val | 0xFFFFF000) == 0xFFFFF0EF ||
      (s->isa.inst.val | 0xFFFF8000) == 0xFFFF80E7) {
    getin(s);
  } else if (s->isa.inst.val == 0x00008067) { // is ret
    getout(s);
  }
}
const char chars[] = {'|', '+', '$', '!', '/'};
void show_position() {
  PositionNode *p = positionNode.next;
  int depth = 0;
  while (p) {
    if (p->isret)
      depth--;
    printf("0x%lx: ", p->position);
    for (int i = 0; i < depth - 1; i++) {
      printf("%c ", chars[i % (sizeof(chars) / sizeof(char))]);
    }
    printf("%s [%s@0x%lx]\n", p->isret ? "ret " : "call", p->funcName,
           p->nextPosition);
    if (!p->isret)
      depth++;
    p = p->next;
  }
}