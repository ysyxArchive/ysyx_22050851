/***************************************************************************************
 * Copyright (c) 2020-2021 Institute of Computing Technology, Chinese Academy of
 *Sciences Copyright (c) 2020-2021 Peng Cheng Laboratory
 *
 * XiangShan is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan
 *PSL v2. You may obtain a copy of Mulan PSL v2 at:
 *          http://license.coscl.org.cn/MulanPSL2
 *
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY
 *KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 *NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 *
 * See the Mulan PSL v2 for more details.
 ***************************************************************************************/
 // origin file source
 // https://github.com/OpenXiangShan/difftest/src/test/csrc/common/lightsss.cpp

#ifndef __LIGHTSSS_H
#define __LIGHTSSS_H

#include <list>
#include <signal.h>
#include <stdint.h>
#include <sys/ipc.h>
#include <sys/prctl.h>
#include <sys/shm.h>
#include <sys/wait.h>
#include <unistd.h>
#define FAIL_EXIT exit(-1);
#define WAIT_INTERVAL 1
// MORE THAN ONE MAY CAUSE BUGS
#define SLOT_SIZE 1

typedef struct shinfo {
  bool flag;
  bool notgood;
  uint64_t endCycles;
  pid_t oldest;
} shinfo;

class ForkShareMemory {
private:
  key_t key_n;
  int shm_id;

public:
  shinfo* info;

  ForkShareMemory();
  ~ForkShareMemory();

  void shwait();
};

const int FORK_OK = 0;
const int FORK_ERROR = 1;
const int WAIT_LAST = 2;
const int WAIT_EXIT = 3;

class LightSSS {
  pid_t pid = -1;
  int slotCnt = 0;
  int waitProcess = 0;
  std::list<pid_t> pidSlot = {};
  ForkShareMemory forkshm;

public:
  int do_fork();
  int wakeup_child(uint64_t cycles);
  bool is_child();
  int do_clear();
  bool is_not_good();
  uint64_t get_end_cycles() { return forkshm.info->endCycles; }
};

#define FORK_PRINTF(format, args...)                                           \
  do {                                                                         \
    printf("[FORK_INFO pid(%d)] " format, getpid(), ##args);                   \
    fflush(stdout);                                                            \
  } while (0);

#endif
