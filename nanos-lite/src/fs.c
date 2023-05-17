#include "ramdisk.h"
#include <fs.h>

typedef size_t (*ReadFn)(void *buf, size_t offset, size_t len);
typedef size_t (*WriteFn)(const void *buf, size_t offset, size_t len);

typedef struct {
  char *name;
  size_t size;
  size_t disk_offset;
  ReadFn read;
  WriteFn write;
} Finfo;

typedef struct openedFileInfo {
  int fd;
  int offset;
  struct openedFileInfo *next;
} OpenedFileInfo;

OpenedFileInfo ofi = {.fd = -1, .next = NULL};

enum { FD_STDIN, FD_STDOUT, FD_STDERR, FD_FB };

size_t invalid_read(void *buf, size_t offset, size_t len) {
  panic("should not reach here");
  return 0;
}

size_t invalid_write(const void *buf, size_t offset, size_t len) {
  panic("should not reach here");
  return 0;
}

/* This is the information about all files in disk. */
static Finfo file_table[] __attribute__((used)) = {
    [FD_STDIN] = {"stdin", 0, 0, invalid_read, invalid_write},
    [FD_STDOUT] = {"stdout", 0, 0, invalid_read, invalid_write},
    [FD_STDERR] = {"stderr", 0, 0, invalid_read, invalid_write},
#include "files.h"
};

int fs_open(const char *filename, int flags, int mode) {
  int ret = -1;
  for (int i = 0; i < LENGTH(file_table); i++) {
    if (strcmp(filename, file_table[i].name) == 0) {
      ret = i;
      break;
    }
  }
  if (ret == -1) {
    Panic("file %s not found", filename);
  }
  OpenedFileInfo *p = &ofi;
  while (p->fd != ret && p->next) {
    p = p->next;
  }
  if (!p->next) {
    OpenedFileInfo *o = (OpenedFileInfo *)malloc(sizeof(OpenedFileInfo));
    o->fd = ret;
    o->next = NULL;
    o->offset = 0;
    p->next = o;
  }
  return ret;
}

int fs_close(int fd) {
  OpenedFileInfo *p = &ofi;
  while (p->next && p->next->fd != fd) {
    p = p->next;
  }
  if (p->next) {
    OpenedFileInfo *o = p->next;
    p->next = p->next->next;
    free(o);
  }
  return 0;
}

int fs_lseek(int fd, int offset, int whence) {
  OpenedFileInfo *p = ofi.next;
  while (p && p->fd != fd) {
    p = p->next;
  }
  if (!p) {
    Log("Warning file %d not opened", fd);
    return -1;
  }
  switch (whence) {
  case SEEK_SET:
    p->offset = offset;
    break;
  case SEEK_CUR:
    p->offset += offset;
    break;
  case SEEK_END:
    p->offset = file_table[fd].size + offset;
    break;
  default:
    return -1;
    break;
  }
  return p->offset;
}

size_t fs_read(int fd, void *buf, size_t count) {
  OpenedFileInfo *p = ofi.next;
  size_t offset;
  while (p && p->fd != fd) {
    p = p->next;
  }
  if (p) {
    offset = p->offset;
    p->offset += count;
  } else {
    Log("Warning file %d not opened", fd);
    return -1;
  }
  return ramdisk_read(buf, file_table[fd].disk_offset + offset, count);
}

void init_fs() {
  // TODO: initialize the size of /dev/fb
}
