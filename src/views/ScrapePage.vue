<template>
  <div class="scrape-page">
    <div class="scrape-page__navbar-wrap">
      <m-navbar>
        <template #title>刮削</template>
        <template #right>
          <button
            v-if="pageState === 'queue'"
            type="button"
            class="scrape-page__history-btn"
            @click="onOpenHistory"
          >历史</button>
          <span v-if="pageState === 'queue' && queueSnapshot.items.length > 0" class="scrape-page__count">
            {{ queueSnapshot.items.length }}
          </span>
        </template>
      </m-navbar>
    </div>
    <div class="m-content scrape-page__content">

      <!-- ========== 队列态 ========== -->
      <template v-if="pageState === 'queue'">
        <div v-if="queueSnapshot.items.length === 0" class="scrape-page__empty">
          <m-empty title="待刮削队列为空" description="请先在歌曲页标记需要刮削的歌曲。" />
        </div>
        <div v-else class="scrape-page__list">
          <m-list :dividers="false">
            <m-list-item
              v-for="item in queueSnapshot.items"
              :key="item.songId"
              :title="getSongTitle(item.songId)"
              :subtitle="getSongArtist(item.songId)"
              :chevron="false"
            >
              <template #after>
                <m-icon-button
                  size="sm"
                  aria-label="从队列移除"
                  @click="onRemoveFromQueue(item.songId)"
                >
                  <component :is="trash" aria-hidden="true" />
                </m-icon-button>
              </template>
            </m-list-item>
          </m-list>
        </div>
        <div v-if="queueSnapshot.items.length > 0" class="scrape-page__actions">
          <m-button
            component="button"
            variant="clear"
            inline
            class="scrape-page__action-btn"
            @click="onClearQueue"
          >
            清空队列
          </m-button>
          <m-button
            component="button"
            variant="fill"
            class="scrape-page__action-btn scrape-page__action-btn--primary"
            @click="onStartMatch"
          >
            开始匹配（{{ queueSnapshot.items.length }} 首）
          </m-button>
        </div>
      </template>

      <!-- ========== 匹配中态 ========== -->
      <template v-else-if="pageState === 'matching'">
        <div class="scrape-page__progress-wrap">
          <m-preloader size="md" />
          <div class="scrape-page__progress-text">
            正在匹配… {{ matchProgress.matched }} / {{ matchProgress.total }}
          </div>
          <div class="scrape-page__progress-bar">
            <div
              class="scrape-page__progress-bar-fill"
              :style="{ width: `${matchProgressPercent}%` }"
            />
          </div>
        </div>
        <div class="scrape-page__actions">
          <m-button
            component="button"
            variant="clear"
            class="scrape-page__action-btn"
            @click="onCancelMatch"
          >
            取消匹配
          </m-button>
        </div>
      </template>

      <!-- ========== 差异预览态 ========== -->
      <template v-else-if="pageState === 'preview'">
        <div class="scrape-page__toolbar">
          <m-checkbox
            :checked="isAllChecked"
            @change="onToggleAll"
          >
            全选（{{ checkedIds.size }}/{{ candidates.length }}）
          </m-checkbox>
          <span class="scrape-page__toolbar-errors" v-if="matchErrors.length > 0">
            {{ matchErrors.length }} 首匹配失败
          </span>
        </div>
        <div class="scrape-page__list scrape-page__list--scrollable">
          <m-list :dividers="true">
            <template v-for="candidate in candidates" :key="candidate.songId">
              <!-- 主行 -->
              <m-list-item
                :title="candidate.song.title"
                :subtitle="`${candidate.song.artist ?? ''} · ${candidate.song.album ?? ''}`"
                :chevron="false"
                :link="true"
                class="scrape-page__preview-row"
                :class="{
                  'scrape-page__preview-row--checked': checkedIds.has(candidate.songId),
                  'scrape-page__preview-row--expanded': expandedId === candidate.songId,
                }"
                @click="onToggleExpand(candidate.songId)"
              >
                <template #media>
                  <m-checkbox
                    :checked="checkedIds.has(candidate.songId)"
                    @change.stop="onToggleCheck(candidate.songId, $event)"
                    @click.stop
                  />
                </template>
                <template #after>
                  <span class="scrape-page__confidence" :class="`scrape-page__confidence--${candidate.overallConfidence}`">
                    ● {{ candidate.overallConfidence === 'high' ? '高' : '低' }}
                  </span>
                </template>
              </m-list-item>
              <!-- 展开：差异详情 -->
              <div v-if="expandedId === candidate.songId" class="scrape-page__detail">
                <!-- 文本维度 -->
                <div class="scrape-page__dim">
                  <div class="scrape-page__dim-label">文本信息</div>
                  <div class="scrape-page__dim-current">
                    当前：{{ candidate.text.current.title || '—' }}
                    <span v-if="candidate.text.current.artist">/ {{ candidate.text.current.artist }}</span>
                    <span v-if="candidate.text.current.album">/ {{ candidate.text.current.album }}</span>
                  </div>
                  <div v-if="candidate.text.candidates.length > 0" class="scrape-page__candidates">
                    <label
                      v-for="(hit, idx) in candidate.text.candidates"
                      :key="`text-${idx}`"
                      class="scrape-page__candidate-item"
                    >
                      <input
                        type="radio"
                        :name="`text-${candidate.songId}`"
                        :checked="getSelectedTextIndex(candidate.songId) === idx"
                        @change="onSelectTextCandidate(candidate.songId, idx)"
                      />
                      <span class="scrape-page__candidate-text">
                        {{ hit.title || '—' }}
                        <span v-if="hit.artist">/ {{ hit.artist }}</span>
                        <span v-if="hit.album">/ {{ hit.album }}</span>
                        <span class="scrape-page__candidate-src">[{{ hit.source }}]</span>
                      </span>
                    </label>
                  </div>
                  <div v-else class="scrape-page__dim-empty">无候选</div>
                </div>
                <!-- 封面维度 -->
                <div class="scrape-page__dim">
                  <div class="scrape-page__dim-label">封面</div>
                  <div class="scrape-page__dim-cover-row">
                    <div class="scrape-page__cover-thumb">
                      <m-cover
                        :src="toDisplayableUri(candidate.cover.currentUri)"
                        :size="40"
                        radius="sm"
                      />
                      <span class="scrape-page__cover-label">当前</span>
                    </div>
                    <template v-if="candidate.cover.candidates.length > 0">
                      <div
                        v-for="(cover, idx) in candidate.cover.candidates"
                        :key="`cover-${idx}`"
                        class="scrape-page__cover-thumb"
                        :class="{ 'scrape-page__cover-thumb--selected': getSelectedCoverIndex(candidate.songId) === idx }"
                      >
                        <input
                          type="radio"
                          :name="`cover-${candidate.songId}`"
                          :checked="getSelectedCoverIndex(candidate.songId) === idx"
                          class="scrape-page__cover-radio"
                          @change="onSelectCoverCandidate(candidate.songId, idx)"
                        />
                        <m-cover
                          :src="cover.remoteUrl"
                          :size="40"
                          radius="sm"
                        />
                        <span class="scrape-page__cover-label">{{ cover.source }}</span>
                      </div>
                    </template>
                    <span v-else class="scrape-page__dim-empty-inline">无候选</span>
                  </div>
                </div>
                <!-- 歌词维度 -->
                <div class="scrape-page__dim">
                  <div class="scrape-page__dim-label">歌词</div>
                  <div class="scrape-page__dim-current">
                    当前格式：{{ candidate.lyrics.currentFormat ?? '无歌词' }}
                  </div>
                  <div v-if="candidate.lyrics.candidates.length > 0" class="scrape-page__candidates">
                    <label
                      v-for="(ly, idx) in candidate.lyrics.candidates"
                      :key="`lyrics-${idx}`"
                      class="scrape-page__candidate-item"
                    >
                      <input
                        type="radio"
                        :name="`lyrics-${candidate.songId}`"
                        :checked="getSelectedLyricsIndex(candidate.songId) === idx"
                        @change="onSelectLyricsCandidate(candidate.songId, idx)"
                      />
                      <span class="scrape-page__candidate-text">
                        {{ ly.format }}
                        <span class="scrape-page__candidate-src">[{{ ly.source }}]</span>
                      </span>
                    </label>
                  </div>
                  <div v-else class="scrape-page__dim-empty">无候选</div>
                </div>
              </div>
            </template>
          </m-list>
        </div>
        <div class="scrape-page__actions">
          <m-button
            component="button"
            variant="clear"
            class="scrape-page__action-btn"
            :disabled="isWritebackBusy"
            @click="onBackToQueue"
          >
            返回队列
          </m-button>
          <m-button
            component="button"
            variant="fill"
            class="scrape-page__action-btn scrape-page__action-btn--primary"
            :disabled="checkedIds.size === 0 || isWritebackBusy"
            @click="onWriteback"
          >
            <span v-if="isWritebackBusy" class="scrape-page__btn-spinner" aria-hidden="true" />
            {{ isWritebackBusy ? '写回中…' : `确认写回（${checkedIds.size} 首）` }}
          </m-button>
        </div>
      </template>

      <!-- ========== 结果态 ========== -->
      <template v-else-if="pageState === 'result'">
        <div class="scrape-page__result-summary">
          <div class="scrape-page__result-stat scrape-page__result-stat--success">
            ✓ {{ resultSummary.success }} 成功
          </div>
          <div class="scrape-page__result-stat scrape-page__result-stat--warning" v-if="resultSummary.fileFailed > 0">
            ⚠ {{ resultSummary.fileFailed }} 文件失败（已入库）
          </div>
          <div class="scrape-page__result-stat scrape-page__result-stat--error" v-if="resultSummary.failed > 0">
            ✗ {{ resultSummary.failed }} 失败
          </div>
          <template v-if="resultSummary.fileFailed + resultSummary.failed > 0">
            <span class="scrape-page__failure-group scrape-page__failure-group--network" v-if="failureGroups.network > 0">网络问题 {{ failureGroups.network }}</span>
            <span class="scrape-page__failure-group scrape-page__failure-group--auth" v-if="failureGroups.auth > 0">认证问题 {{ failureGroups.auth }}</span>
            <span class="scrape-page__failure-group scrape-page__failure-group--upload" v-if="failureGroups.upload > 0">上传失败 {{ failureGroups.upload }}</span>
            <span class="scrape-page__failure-group" v-if="failureGroups.other > 0">其他 {{ failureGroups.other }}</span>
          </template>
        </div>
        <div class="scrape-page__list scrape-page__list--scrollable">
          <m-list :dividers="true">
            <m-list-item
              v-for="result in writebackResults"
              :key="result.songId"
              :title="getSongTitle(result.songId)"
              :subtitle="getResultSubtitle(result)"
              :chevron="false"
              :class="`scrape-page__result-row scrape-page__result-row--${result.status}`"
            >
              <template #media>
                <span class="scrape-page__result-icon">
                  {{ result.status === 'success' ? '✓' : result.status === 'file-failed' ? '⚠' : '✗' }}
                </span>
              </template>
            </m-list-item>
          </m-list>
        </div>
        <div class="scrape-page__actions">
          <m-button
            v-if="resultSummary.failed + resultSummary.fileFailed > 0"
            component="button"
            variant="clear"
            class="scrape-page__action-btn"
            :disabled="resultBusy"
            @click="onRetryFailed"
          >
            <span v-if="isRetryBusy" class="scrape-page__btn-spinner" aria-hidden="true" />
            {{ isRetryBusy ? '重试中…' : '重试失败项' }}
          </m-button>
          <m-button
            v-if="resultSummary.success > 0 || resultSummary.fileFailed > 0"
            component="button"
            variant="clear"
            class="scrape-page__action-btn scrape-page__action-btn--danger"
            :disabled="resultBusy"
            @click="onRevert"
          >
            {{ isRevertBusy ? '撤销中…' : '撤销本次刮削' }}
          </m-button>
          <m-button
            component="button"
            variant="fill"
            class="scrape-page__action-btn scrape-page__action-btn--primary"
            :disabled="resultBusy"
            @click="onBackToQueue"
          >
            返回队列
          </m-button>
        </div>
      </template>

      <!-- ========== 历史弹层 ========== -->
      <m-sheet :opened="isHistoryOpen" @backdropclick="isHistoryOpen = false">
        <div class="scrape-page__history">
          <div class="scrape-page__history-header">刮削历史</div>
          <div v-if="historyEntries.length === 0" class="scrape-page__history-empty">暂无刮削记录</div>
          <div v-else class="scrape-page__history-list">
            <div
              v-for="entry in historyEntries"
              :key="entry.id"
              class="scrape-page__history-row"
            >
              <span
                class="scrape-page__result-icon scrape-page__result-icon--sm"
                :class="`scrape-page__result-icon--${entry.status}`"
              >
                {{ entry.status === 'success' ? '✓' : entry.status === 'file-failed' ? '⚠' : '✗' }}
              </span>
              <div class="scrape-page__history-main">
                <div class="scrape-page__history-title">{{ entry.songTitle }}</div>
                <div
                  v-if="entry.songArtist || entry.status !== 'success'"
                  class="scrape-page__history-sub"
                >{{ getHistoryRowSub(entry) }}</div>
              </div>
              <span class="scrape-page__history-time">{{ formatRelativeTime(entry.at) }}</span>
            </div>
          </div>
          <div class="scrape-page__history-footer">
            <m-button
              component="button"
              variant="clear"
              inline
              class="scrape-page__history-clear"
              :disabled="historyEntries.length === 0"
              @click="isHistoryClearConfirmOpen = true"
            >清空历史</m-button>
          </div>
        </div>
      </m-sheet>

      <!-- 清空历史确认 -->
      <m-dialog :opened="isHistoryClearConfirmOpen" title="清空刮削历史">
        <p class="scrape-page__revert-notice">所有刮削记录将被删除，且不可恢复。</p>
        <template #buttons>
          <m-dialog-button @click="isHistoryClearConfirmOpen = false">取消</m-dialog-button>
          <m-dialog-button strong @click="onConfirmClearHistory">清空</m-dialog-button>
        </template>
      </m-dialog>

      <!-- 撤销确认提示 -->
      <m-dialog :opened="isRevertConfirmOpen" title="撤销本次刮削">
        <p class="scrape-page__revert-notice">
          曲库中的元数据将恢复为刮削前的值。但<strong>音频文件中已写入的标签不可逆</strong>。
        </p>
        <template #buttons>
          <m-dialog-button @click="isRevertConfirmOpen = false">取消</m-dialog-button>
          <m-dialog-button strong :disabled="isRevertBusy" @click="onConfirmRevert">
            {{ isRevertBusy ? '撤销中…' : '确认撤销' }}
          </m-dialog-button>
        </template>
      </m-dialog>

      <m-toast :opened="toast.visible" position="center">
        {{ toast.message }}
      </m-toast>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onUnmounted } from 'vue'
import { Capacitor } from '@capacitor/core'
import { trash } from '@/icons'
import {
  MButton, MCheckbox, MCover, MDialog, MDialogButton, MEmpty,
  MIconButton, MList, MListItem, MNavbar,
  MPreloader, MSheet, MToast,
} from '@/components/ui'
import {
  loadScrapeHistory,
  clearScrapeHistory,
  onScrapeHistoryChanged,
  type ScrapeHistoryEntry,
} from '@/features/scrape/history'
import {
  loadScrapeQueue,
  removeScrapeSongs,
  clearScrapeQueue,
  type ScrapeQueueSnapshot,
} from '@/features/scrape/queue'
import { loadSongs } from '@/features/library/storage'
import { matchScrapeQueue, type ScrapeCandidate, type ScrapeMatchError, type MatchProgress } from '@/features/scrape/matcher'
import { applyScrapeChanges, revertScrapeJournal, type ScrapeChanges, type WritebackResult } from '@/features/scrape/writeback'
import { classifyWritebackFailure, describeWritebackFailure } from '@/features/scrape/failure-copy'

// ── 页面状态机 ────────────────────────────────────────────

type PageState = 'queue' | 'matching' | 'preview' | 'result'

const pageState = ref<PageState>('queue')

// ── 队列 ──────────────────────────────────────────────────

const queueSnapshot = ref<ScrapeQueueSnapshot>(loadScrapeQueue())

const refreshQueue = (): void => {
  queueSnapshot.value = loadScrapeQueue()
}

const songCache = computed(() => {
  const map = new Map<string, ReturnType<typeof loadSongs>[number]>()
  for (const song of loadSongs()) {
    map.set(song.id, song)
  }
  return map
})

const getSongTitle = (songId: string): string => {
  return songCache.value.get(songId)?.title ?? songId
}

const getSongArtist = (songId: string): string => {
  const song = songCache.value.get(songId)
  return song ? [song.artist, song.album].filter(Boolean).join(' · ') : ''
}

const onRemoveFromQueue = (songId: string): void => {
  removeScrapeSongs([songId])
  refreshQueue()
}

const onClearQueue = (): void => {
  clearScrapeQueue()
  refreshQueue()
}

const onBackToQueue = (): void => {
  refreshQueue()
  pageState.value = 'queue'
}

// ── 匹配 ──────────────────────────────────────────────────

const matchProgress = ref<MatchProgress>({ matched: 0, total: 0 })
const matchProgressPercent = computed(() => {
  if (matchProgress.value.total === 0) return 0
  return Math.round((matchProgress.value.matched / matchProgress.value.total) * 100)
})
const candidates = ref<ScrapeCandidate[]>([])
const matchErrors = ref<ScrapeMatchError[]>([])
let matchAbortController: AbortController | null = null

const onStartMatch = async (): Promise<void> => {
  const queue = loadScrapeQueue()
  if (queue.items.length === 0) {
    return
  }
  pageState.value = 'matching'
  matchProgress.value = { matched: 0, total: queue.items.length }
  candidates.value = []
  matchErrors.value = []

  matchAbortController = new AbortController()
  try {
    const result = await matchScrapeQueue(queue, {
      signal: matchAbortController.signal,
      onProgress: (progress) => {
        matchProgress.value = progress
      },
    })
    if (matchAbortController.signal.aborted) return
    candidates.value = result.candidates
    matchErrors.value = result.errors
    if (result.candidates.length === 0) {
      showToast('所有歌曲匹配失败，请检查网络后重试')
      refreshQueue()
      pageState.value = 'queue'
    } else {
      // 初始化选择状态
      initSelections()
      pageState.value = 'preview'
    }
  } catch {
    if (matchAbortController?.signal.aborted) return
    showToast('匹配过程出错')
    refreshQueue()
    pageState.value = 'queue'
  } finally {
    matchAbortController = null
  }
}

const onCancelMatch = (): void => {
  matchAbortController?.abort()
  matchAbortController = null
  refreshQueue()
  pageState.value = 'queue'
}

// ── 差异预览 & 选择 ───────────────────────────────────────

/** 勾选状态：songId → checked */
const checkedIds = ref<Set<string>>(new Set())
/** 文本候选选择：songId → candidate index */
const selectedTextIndices = ref<Map<string, number>>(new Map())
/** 封面候选选择：songId → candidate index */
const selectedCoverIndices = ref<Map<string, number>>(new Map())
/** 歌词候选选择：songId → candidate index */
const selectedLyricsIndices = ref<Map<string, number>>(new Map())
/** 当前展开行 */
const expandedId = ref<string | null>(null)

const isAllChecked = computed(() => {
  return candidates.value.length > 0 && checkedIds.value.size === candidates.value.length
})

const initSelections = (): void => {
  const checked = new Set<string>()
  const textMap = new Map<string, number>()
  const coverMap = new Map<string, number>()
  const lyricsMap = new Map<string, number>()

  for (const c of candidates.value) {
    // 高置信默认勾选
    if (c.defaultChecked) {
      checked.add(c.songId)
    }
    // 各维度默认候选
    if (c.text.defaultIndex >= 0) textMap.set(c.songId, c.text.defaultIndex)
    if (c.cover.defaultIndex >= 0) coverMap.set(c.songId, c.cover.defaultIndex)
    if (c.lyrics.defaultIndex >= 0) lyricsMap.set(c.songId, c.lyrics.defaultIndex)
  }

  checkedIds.value = checked
  selectedTextIndices.value = textMap
  selectedCoverIndices.value = coverMap
  selectedLyricsIndices.value = lyricsMap
}

const onToggleAll = (): void => {
  if (isAllChecked.value) {
    checkedIds.value = new Set()
  } else {
    checkedIds.value = new Set(candidates.value.map((c) => c.songId))
  }
}

const onToggleCheck = (songId: string, event: Event): void => {
  const checked = (event.target as HTMLInputElement).checked
  const next = new Set(checkedIds.value)
  if (checked) {
    next.add(songId)
  } else {
    next.delete(songId)
  }
  checkedIds.value = next
}

const onToggleExpand = (songId: string): void => {
  expandedId.value = expandedId.value === songId ? null : songId
}

const getSelectedTextIndex = (songId: string): number => {
  return selectedTextIndices.value.get(songId) ?? -1
}

const getSelectedCoverIndex = (songId: string): number => {
  return selectedCoverIndices.value.get(songId) ?? -1
}

const getSelectedLyricsIndex = (songId: string): number => {
  return selectedLyricsIndices.value.get(songId) ?? -1
}

const onSelectTextCandidate = (songId: string, idx: number): void => {
  const next = new Map(selectedTextIndices.value)
  next.set(songId, idx)
  selectedTextIndices.value = next
  // 自动勾选该行
  if (!checkedIds.value.has(songId)) {
    const c = new Set(checkedIds.value)
    c.add(songId)
    checkedIds.value = c
  }
}

const onSelectCoverCandidate = (songId: string, idx: number): void => {
  const next = new Map(selectedCoverIndices.value)
  next.set(songId, idx)
  selectedCoverIndices.value = next
  if (!checkedIds.value.has(songId)) {
    const c = new Set(checkedIds.value)
    c.add(songId)
    checkedIds.value = c
  }
}

const onSelectLyricsCandidate = (songId: string, idx: number): void => {
  const next = new Map(selectedLyricsIndices.value)
  next.set(songId, idx)
  selectedLyricsIndices.value = next
  if (!checkedIds.value.has(songId)) {
    const c = new Set(checkedIds.value)
    c.add(songId)
    checkedIds.value = c
  }
}

/** 根据用户选择构建 changesMap */
const buildChangesMap = (): Map<string, ScrapeChanges> => {
  const map = new Map<string, ScrapeChanges>()
  for (const candidate of candidates.value) {
    if (!checkedIds.value.has(candidate.songId)) continue
    const changes: ScrapeChanges = {}
    // 文本维度
    const textIdx = selectedTextIndices.value.get(candidate.songId) ?? candidate.text.defaultIndex
    if (textIdx >= 0 && textIdx < candidate.text.candidates.length) {
      const hit = candidate.text.candidates[textIdx]
      if (hit.title) changes.title = hit.title
      if (hit.artist) changes.artist = hit.artist
      if (hit.album) changes.album = hit.album
    }
    // 封面维度
    const coverIdx = selectedCoverIndices.value.get(candidate.songId) ?? candidate.cover.defaultIndex
    if (coverIdx >= 0 && coverIdx < candidate.cover.candidates.length) {
      const cover = candidate.cover.candidates[coverIdx]
      changes.coverRemoteUrl = cover.remoteUrl
      changes.coverUri = cover.remoteUrl // 标记有封面变更
    }
    // 歌词维度
    const lyricsIdx = selectedLyricsIndices.value.get(candidate.songId) ?? candidate.lyrics.defaultIndex
    if (lyricsIdx >= 0 && lyricsIdx < candidate.lyrics.candidates.length) {
      const ly = candidate.lyrics.candidates[lyricsIdx]
      changes.lyrics = ly.text
      changes.lyricsFormat = ly.format
    }
    map.set(candidate.songId, changes)
  }
  return map
}

// ── 写回 ──────────────────────────────────────────────────

const writebackResults = ref<WritebackResult[]>([])
const currentJournalId = ref<string | null>(null)
/** 写回/重试/撤销进行中的加载态（防重复点击 + 按钮反馈） */
const isWritebackBusy = ref(false)
const isRetryBusy = ref(false)
const isRevertBusy = ref(false)
/** 结果态任一操作进行中：期间禁用结果区全部按钮（防中途跳转） */
const resultBusy = computed(() => isWritebackBusy.value || isRetryBusy.value || isRevertBusy.value)

const resultSummary = computed(() => {
  let success = 0
  let fileFailed = 0
  let failed = 0
  for (const r of writebackResults.value) {
    if (r.status === 'success') success++
    else if (r.status === 'file-failed') fileFailed++
    else failed++
  }
  return { success, fileFailed, failed }
})

const getResultSubtitle = (result: WritebackResult): string => {
  if (result.status === 'success') return '写回成功'
  const reason = describeWritebackFailure(result)
  // file-failed 时值仍已入库（来源标 scrape），在具体原因前补充入库语义
  if (result.status === 'file-failed') return `值已入库：${reason}`
  return reason
}

/** 失败原因分组计数（网络/认证/上传/其他），仅统计失败行 */
const failureGroups = computed(() => {
  const groups = { network: 0, auth: 0, upload: 0, other: 0 }
  for (const r of writebackResults.value) {
    if (r.status === 'success') continue
    groups[classifyWritebackFailure(r.fileResult.code)]++
  }
  return groups
})

const onWriteback = async (): Promise<void> => {
  if (isWritebackBusy.value) return
  const changesMap = buildChangesMap()
  if (changesMap.size === 0) {
    showToast('请至少勾选一首歌曲')
    return
  }
  // 预期管理：写回含下载+写标签+PUT，云端歌曲可能耗时数分钟
  showToast('开始写回，云端歌曲可能需要较长时间…', 4000)
  isWritebackBusy.value = true
  try {
    const { journalId, results } = await applyScrapeChanges(
      candidates.value,
      checkedIds.value,
      changesMap,
    )
    currentJournalId.value = journalId
    writebackResults.value = results
    pageState.value = 'result'
    // 清空队列中已处理的
    const processedIds = results.map((r) => r.songId)
    removeScrapeSongs(processedIds)
  } catch {
    showToast('写回过程出错，请重试')
  } finally {
    isWritebackBusy.value = false
  }
}

// ── 重试 ──────────────────────────────────────────────────

const onRetryFailed = async (): Promise<void> => {
  if (isRetryBusy.value) return
  // 全量可重试：failed（写库前异常）与 file-failed（最常见=网络抖动，值仍已入库）都纳入
  const failedIds = new Set(
    writebackResults.value
      .filter((r) => r.status === 'failed' || r.status === 'file-failed')
      .map((r) => r.songId),
  )
  if (failedIds.size === 0) return

  // 过滤出失败候选
  const retryCandidates = candidates.value.filter((c) => failedIds.has(c.songId))
  const retryChecked = new Set(retryCandidates.map((c) => c.songId))
  const changesMap = buildChangesMap()

  // 仅传入失败行的 changes
  const retryChangesMap = new Map<string, ScrapeChanges>()
  for (const [songId, changes] of changesMap) {
    if (failedIds.has(songId)) {
      retryChangesMap.set(songId, changes)
    }
  }

  isRetryBusy.value = true
  try {
    const { journalId, results } = await applyScrapeChanges(
      retryCandidates,
      retryChecked,
      retryChangesMap,
    )
    currentJournalId.value = journalId
    // 合并结果：成功的保留，失败的更新
    const merged = [...writebackResults.value]
    for (const newResult of results) {
      const idx = merged.findIndex((r) => r.songId === newResult.songId)
      if (idx >= 0) {
        merged[idx] = newResult
      } else {
        merged.push(newResult)
      }
    }
    writebackResults.value = merged
  } catch {
    showToast('重试过程出错')
  } finally {
    isRetryBusy.value = false
  }
}

// ── 撤销 ──────────────────────────────────────────────────

const isRevertConfirmOpen = ref(false)

const onRevert = (): void => {
  if (!currentJournalId.value) return
  isRevertConfirmOpen.value = true
}

const onConfirmRevert = async (): Promise<void> => {
  if (isRevertBusy.value) return
  if (!currentJournalId.value) return
  isRevertBusy.value = true
  try {
    const { reverted } = revertScrapeJournal(currentJournalId.value)
    isRevertConfirmOpen.value = false
    if (reverted > 0) {
      showToast(`已恢复 ${reverted} 首歌曲的曲库值（文件标签不可逆）`)
    } else {
      showToast('未找到可恢复的记录')
    }
  } finally {
    isRevertBusy.value = false
  }
}

// ── 历史 ──────────────────────────────────────────────

const isHistoryOpen = ref(false)
const isHistoryClearConfirmOpen = ref(false)
const historyEntries = ref<ScrapeHistoryEntry[]>([])
let historyUnsubscribe: (() => void) | null = null

const refreshHistory = (): void => {
  if (isHistoryOpen.value) {
    historyEntries.value = loadScrapeHistory()
  }
}

// 打开时拉取最新；写回落史后事件驱动刷新
historyUnsubscribe = onScrapeHistoryChanged(refreshHistory)

const onOpenHistory = (): void => {
  historyEntries.value = loadScrapeHistory()
  isHistoryOpen.value = true
}

const getHistoryRowSub = (entry: ScrapeHistoryEntry): string => {
  const parts: string[] = []
  if (entry.songArtist) parts.push(entry.songArtist)
  if (entry.status !== 'success') {
    const prefix = entry.status === 'file-failed' ? '值已入库：' : ''
    parts.push(`${prefix}${entry.failureReason ?? '写回失败'}`)
  }
  return parts.join(' · ')
}

/** 相对时间：刚刚/N 分钟前/N 小时前/N 天前/日期 */
const formatRelativeTime = (iso: string): string => {
  const time = new Date(iso).getTime()
  if (Number.isNaN(time)) return ''
  const diffMs = Date.now() - time
  const minute = 60 * 1000
  const hour = 60 * minute
  const day = 24 * hour
  if (diffMs < minute) return '刚刚'
  if (diffMs < hour) return `${Math.floor(diffMs / minute)} 分钟前`
  if (diffMs < day) return `${Math.floor(diffMs / hour)} 小时前`
  if (diffMs < 7 * day) return `${Math.floor(diffMs / day)} 天前`
  return new Date(iso).toLocaleDateString()
}

const onConfirmClearHistory = (): void => {
  isHistoryClearConfirmOpen.value = false
  clearScrapeHistory()
  historyEntries.value = []
}

// ── Toast ─────────────────────────────────────────────────

const toast = ref<{ visible: boolean; message: string }>({ visible: false, message: '' })
let toastTimer: ReturnType<typeof setTimeout> | null = null
const showToast = (message: string, duration = 2000): void => {
  toast.value = { visible: true, message }
  if (toastTimer) clearTimeout(toastTimer)
  toastTimer = setTimeout(() => {
    toast.value = { visible: false, message: '' }
  }, duration)
}

// ── 工具函数 ──────────────────────────────────────────────

const toDisplayableUri = (uri: string | undefined): string => {
  if (!uri) return ''
  const normalizedUri = uri.trim().toLowerCase()
  if (normalizedUri.startsWith('data:') || normalizedUri.startsWith('blob:') || normalizedUri.includes(';base64,')) {
    return ''
  }
  if (normalizedUri.startsWith('http://') || normalizedUri.startsWith('https://')) {
    return uri
  }
  return Capacitor.convertFileSrc(uri)
}

// ── 监听队列变化 ──────────────────────────────────────────

let queueUnsubscribe: (() => void) | null = null
const setupQueueListener = (): void => {
  if (typeof window === 'undefined') return
  const handler = (): void => {
    if (pageState.value === 'queue') {
      refreshQueue()
    }
  }
  window.addEventListener('muses:scrape-queue-updated', handler)
  queueUnsubscribe = () => window.removeEventListener('muses:scrape-queue-updated', handler)
}

setupQueueListener()
onUnmounted(() => {
  queueUnsubscribe?.()
  historyUnsubscribe?.()
  if (toastTimer) clearTimeout(toastTimer)
})
</script>

<style scoped lang="scss">
.scrape-page {
  position: relative;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  height: 100%;

  &__navbar-wrap {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    z-index: 20;

    :deep(.m-navbar) {
      padding-top: var(--m-navbar-pt, 16px);
      background: var(--m-navbar-glass-bg);
      box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.65);
      -webkit-backdrop-filter: blur(20px);
      backdrop-filter: blur(20px);
    }
    :deep(.m-navbar__bg) {
      background-color: transparent;
    }
  }

  &__content {
    overflow: hidden;
    display: flex;
    flex-direction: column;
  }

  &__count {
    font-size: 14px;
    color: var(--m-primary);
    font-weight: 500;
  }

  // ── 历史按钮（navbar 右侧文字按钮）──
  &__history-btn {
    appearance: none;
    border: none;
    background: none;
    padding: 4px 8px;
    margin-right: 4px;
    font-size: 14px;
    color: var(--m-primary);
    cursor: pointer;
  }

  // ── 历史弹层 ──
  &__history {
    display: flex;
    flex-direction: column;
    max-height: 70vh;
    box-sizing: border-box;
  }

  &__history-header {
    padding: 20px 16px 12px;
    font-size: var(--m-font-size-lg);
    font-weight: 600;
    text-align: center;
  }

  &__history-empty {
    padding: 32px 16px;
    text-align: center;
    font-size: 14px;
    color: var(--m-text-2);
  }

  &__history-list {
    flex: 1;
    min-height: 0;
    overflow-y: auto;
    padding: 0 16px;
  }

  &__history-row {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 10px 0;

    & + & {
      border-top: 1px solid var(--m-hairline);
    }
  }

  &__history-main {
    flex: 1;
    min-width: 0;
  }

  &__history-title {
    font-size: 15px;
    color: var(--m-text);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__history-sub {
    margin-top: 2px;
    font-size: 12px;
    color: var(--m-text-2);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__history-time {
    flex-shrink: 0;
    font-size: 12px;
    color: var(--m-text-3, #999);
  }

  &__history-footer {
    padding: 8px 16px calc(12px + var(--safe-area-inset-bottom, env(safe-area-inset-bottom, 0px)));
    border-top: 1px solid var(--m-hairline);
    display: flex;
    justify-content: center;
  }

  &__history-clear {
    color: var(--m-danger, #ff3b30);
  }

  &__result-icon--sm {
    width: 24px;
    height: 24px;
    font-size: 12px;
  }

  // ── 空态 ──
  &__empty {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    padding-top: calc(var(--m-navbar-pt, 16px) + 44px);
  }

  // ── 队列列表 ──
  &__list {
    flex: 1;
    overflow-y: auto;
    padding-top: calc(var(--m-navbar-pt, 16px) + 44px);
    padding-bottom: var(--m-content-pb);

    &--scrollable {
      overflow-y: auto;
    }
  }

  // ── 匹配进度 ──
  &__progress-wrap {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 16px;
    padding-top: calc(var(--m-navbar-pt, 16px) + 44px);
  }

  &__progress-text {
    font-size: 15px;
    color: var(--m-text-2);
  }

  &__progress-bar {
    width: 240px;
    height: 4px;
    border-radius: 2px;
    background: var(--m-surface-2);
    overflow: hidden;
  }

  &__progress-bar-fill {
    height: 100%;
    border-radius: 2px;
    background: var(--m-primary);
    transition: width 0.3s ease;
  }

  // ── 预览工具栏 ──
  &__toolbar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 8px 16px;
    padding-top: calc(var(--m-navbar-pt, 16px) + 44px + 8px);
    font-size: 14px;
    color: var(--m-text-2);
    border-bottom: 1px solid var(--m-hairline);
  }

  &__toolbar-errors {
    font-size: 13px;
    color: var(--m-danger, #ff3b30);
  }

  // ── 预览行 ──
  &__preview-row {
    transition: background-color 0.15s ease;

    &--checked {
      background-color: rgba(var(--m-primary-rgb), 0.06);
    }
  }

  // ── 置信度徽标 ──
  &__confidence {
    font-size: 12px;
    font-weight: 500;
    white-space: nowrap;

    &--high {
      color: #34c759;
    }
    &--low {
      color: #ff9500;
    }
  }

  // ── 展开详情 ──
  &__detail {
    padding: 0 16px 12px;
    border-bottom: 1px solid var(--m-hairline);
  }

  &__dim {
    margin-top: 8px;
  }

  &__dim-label {
    font-size: 12px;
    font-weight: 600;
    color: var(--m-text-2);
    text-transform: uppercase;
    letter-spacing: 0.5px;
    margin-bottom: 4px;
  }

  &__dim-current {
    font-size: 13px;
    color: var(--m-text-2);
    margin-bottom: 6px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__dim-empty {
    font-size: 13px;
    color: var(--m-text-3, #999);
    font-style: italic;
  }

  &__dim-empty-inline {
    font-size: 13px;
    color: var(--m-text-3, #999);
    font-style: italic;
  }

  // ── 候选列表 ──
  &__candidates {
    display: flex;
    flex-direction: column;
    gap: 4px;
  }

  &__candidate-item {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 13px;
    color: var(--m-text);
    cursor: pointer;
    padding: 4px 0;

    input[type="radio"] {
      flex-shrink: 0;
    }
  }

  &__candidate-text {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    min-width: 0;
  }

  &__candidate-src {
    color: var(--m-text-3, #999);
    font-size: 11px;
  }

  // ── 封面缩略图行 ──
  &__dim-cover-row {
    display: flex;
    gap: 10px;
    align-items: flex-start;
    overflow-x: auto;
    padding: 4px 0;
  }

  &__cover-thumb {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 2px;
    flex-shrink: 0;
    cursor: pointer;
    position: relative;

    &--selected {
      :deep(.m-cover) {
        outline: 2px solid var(--m-primary);
        outline-offset: 1px;
      }
    }
  }

  &__cover-radio {
    position: absolute;
    top: 2px;
    right: 2px;
    z-index: 1;
    width: 14px;
    height: 14px;
  }

  &__cover-label {
    font-size: 10px;
    color: var(--m-text-3, #999);
  }

  // ── 底部操作栏 ──
  &__actions {
    display: flex;
    gap: 8px;
    padding: 12px 16px;
    padding-bottom: calc(12px + var(--m-content-pb, 0px));
    border-top: 1px solid var(--m-hairline);
    background: var(--m-surface-1);
  }

  &__action-btn {
    flex: 1;
    height: 40px;
    font-size: 15px;

    &--primary {
      flex: 2;
    }

    &--danger {
      color: var(--m-danger, #ff3b30);
    }
  }

  // ── 按钮内转圈指示（busy 态）──
  &__btn-spinner {
    display: inline-block;
    width: 14px;
    height: 14px;
    margin-right: 6px;
    vertical-align: -2px;
    border: 2px solid rgba(var(--m-primary-rgb), 0.3);
    border-top-color: currentColor;
    border-radius: 50%;
    animation: scrape-btn-spin 0.8s linear infinite;
  }

  // ── 结果态 ──
  &__result-summary {
    display: flex;
    gap: 16px;
    padding: 12px 16px;
    padding-top: calc(var(--m-navbar-pt, 16px) + 44px + 12px);
    font-size: 14px;
    font-weight: 500;
    border-bottom: 1px solid var(--m-hairline);
    flex-wrap: wrap;
  }

  &__result-stat {
    &--success { color: #34c759; }
    &--warning { color: #ff9500; }
    &--error { color: var(--m-danger, #ff3b30); }
  }

  &__failure-group {
    font-size: 13px;
    font-weight: 400;
    color: var(--m-text-2);
    padding: 2px 8px;
    border-radius: var(--m-radius-sm);
    background: var(--m-surface-2);

    &--network,
    &--auth,
    &--upload {
      color: #ff9500;
      background: rgba(255, 149, 0, 0.12);
    }
  }

  &__result-icon {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 28px;
    height: 28px;
    border-radius: 50%;
    font-size: 14px;
    font-weight: 600;
  }

  &__result-icon--success {
    background: rgba(52, 199, 89, 0.12);
    color: #34c759;
  }

  &__result-icon--file-failed {
    background: rgba(255, 149, 0, 0.12);
    color: #ff9500;
  }

  &__result-icon--failed {
    background: rgba(255, 59, 48, 0.12);
    color: var(--m-danger, #ff3b30);
  }

  &__result-row--success &__result-icon {
    background: rgba(52, 199, 89, 0.12);
    color: #34c759;
  }

  &__result-row--file-failed &__result-icon {
    background: rgba(255, 149, 0, 0.12);
    color: #ff9500;
  }

  &__result-row--failed &__result-icon {
    background: rgba(255, 59, 48, 0.12);
    color: var(--m-danger, #ff3b30);
  }

  // ── 撤销确认 ──
  &__revert-notice {
    font-size: 15px;
    line-height: 1.5;
    color: var(--m-text);
    margin: 0;
    padding: 0 16px;
  }
}

@keyframes scrape-btn-spin {
  to {
    transform: rotate(360deg);
  }
}

:global(.dark .scrape-page .m-navbar) {
  background: var(--m-navbar-glass-bg);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.1);
}
</style>
