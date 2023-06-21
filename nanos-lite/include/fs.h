#ifndef __FS_H__
#define __FS_H__

#include <common.h>

#ifndef SEEK_SET
enum { SEEK_SET, SEEK_CUR, SEEK_END };
#endif
enum { FD_STDIN, FD_STDOUT, FD_STDERR, FD_FB };
int fs_open(const char *filename, int flags, int mode);
int fs_close(int fd);
size_t fs_read(int fd, void *buf, size_t count);
size_t fs_write(int fd, void *buf, size_t count);
int fs_lseek(int fd, int offset, int whence);
char *get_file_name(int fd);
#endif
