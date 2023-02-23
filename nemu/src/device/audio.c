/***************************************************************************************
 * Copyright (c) 2014-2022 Zihao Yu, Nanjing University
 *
 * NEMU is licensed under Mulan PSL v2.
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

#include <SDL2/SDL.h>
#include <common.h>
#include <device/map.h>

enum {
  reg_freq,
  reg_channels,
  reg_samples,
  reg_sbuf_size,
  reg_init,
  reg_count,
  nr_reg
};

SDL_AudioSpec s;

static uint8_t* sbuf = NULL;
static uint32_t* audio_base = NULL;
static int bufoffset = 0;
void audio_callback(void* userdata, Uint8* stream, int len) {
  int count = audio_base[reg_count];
  memcpy(stream, sbuf + bufoffset, len);
  if (bufoffset + len < count) {
    bufoffset += len;
  } else {
    memset(stream + count - bufoffset, s.silence, len + bufoffset - count);
    audio_base[reg_count] = 0;
    bufoffset = 0;
  }
}

static void audio_io_handler(uint32_t offset, int len, bool is_write) {
  if (audio_base[reg_init]) {
    s.format = AUDIO_S16SYS;
    s.size = CONFIG_SB_SIZE;
    s.callback = audio_callback;
    s.userdata = NULL;
    s.freq = audio_base[reg_freq];
    s.channels = audio_base[reg_channels];
    s.samples = audio_base[reg_samples];
    int ret = SDL_OpenAudio(&s, NULL);
    Assert(ret == 0, "%s", SDL_GetError());
    SDL_PauseAudio(0);
    audio_base[reg_init] = false;
  }
}

void init_audio() {
  uint32_t space_size = sizeof(uint32_t) * nr_reg;
  audio_base = (uint32_t*)new_space(space_size);
  audio_base[reg_sbuf_size] = CONFIG_SB_SIZE;
  audio_base[reg_init] = false;
#ifdef CONFIG_HAS_PORT_IO
  add_pio_map("audio", CONFIG_AUDIO_CTL_PORT, audio_base, space_size,
              audio_io_handler);
#else
  add_mmio_map("audio", CONFIG_AUDIO_CTL_MMIO, audio_base, space_size,
               audio_io_handler);
#endif

  sbuf = (uint8_t*)new_space(CONFIG_SB_SIZE);
  add_mmio_map("audio-sbuf", CONFIG_SB_ADDR, sbuf, CONFIG_SB_SIZE, NULL);

  SDL_InitSubSystem(SDL_INIT_AUDIO);
}
