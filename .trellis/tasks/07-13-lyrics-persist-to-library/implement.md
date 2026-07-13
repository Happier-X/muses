# 实施清单

1. `library/types`：`LyricsSource` + `lyricsFormat`
2. `library/storage`：校验与 merge
3. `player/types`：snapshot 带 `lyricsFormat`
4. `controller`：`lyricsRank` / `shouldPersistOnlineLyrics`；命中后写回；init 用库 format
5. 单测 lyrics.spec / library.spec
6. features-player + state-management
7. lint / tsc / vitest
