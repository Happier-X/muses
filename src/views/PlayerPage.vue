<template>
  <m-popup :opened="playerOverlayVisible" fullscreen transparent>
    <div
      class="player-page__overlay player-overlay"
      :class="{ 'player-page--tablet': isTabletLayout, 'player-overlay--tablet': isTabletLayout }"
      @touchstart.passive="onTouchStart"
      @touchmove="onTouchMove"
      @touchend="onTouchEnd"
      @touchcancel="onTouchEnd"
    >
      <div
        ref="dragLayerRef"
        class="player-page__drag-layer"
        :class="{ 'is-dragging': isDraggingVertically }"
        :style="{ transform: `translateY(${dragOffsetY}px)` }"
      >
        <!-- 背景与歌词解耦：切歌暂无词时不卸载，避免闪默认底（#20） -->
        <div v-if="showAlbumBackground" class="player-page__bg">
          <BackgroundRender
            :key="backgroundAlbumSrc || 'no-album'"
            class="player-page__bg-render"
            :album="backgroundAlbumSrc || undefined"
            :album-is-video="false"
            :flow-speed="2"
            :has-lyric="hasLyrics"
            :renderer="meshGradientRenderer"
          />
        </div>
        <div
          class="fallback-background player-page__fallback"
          :class="{ 'player-page__fallback--hidden': showAlbumBackground }"
        />

        <section v-if="!playerState.currentSong" class="empty-state player-page__empty">
          <div class="placeholder-cover player-page__empty-icon">♪</div>
          <h1>暂无播放歌曲</h1>
          <p>从歌曲列表选择一首音乐后，即可进入沉浸式播放。</p>
        </section>

        <template v-else>
          <!-- 固定头部（椒盐式：歌名/艺术家常驻，左右滑切面板不移动；平板由面板内头部承担） -->
          <div class="player-page__song-head player-page__song-head--fixed">
            <h1 class="player-page__song-title">{{ playerState.currentSong.title }}</h1>
            <p v-if="lyricArtist" class="player-page__song-artist">{{ lyricArtist }}</p>
          </div>

          <motion.div
            class="panels player-page__panels"
            :animate="{ transform: `translateX(-${activePanel * 50}%)` }"
            :transition="{ duration: 0.22, ease: 'easeOut' }"
          >
          <section class="panel info-panel player-page__info-panel" aria-label="播放控制页">
            <div class="info-panel-inner player-page__info-inner">
              <!-- 顶部歌名/歌手（仅平板 ≥768px 显示；手机由固定头部承担，避免重复） -->
              <div class="player-page__song-head player-page__song-head--in-panel">
                <h1 class="player-page__song-title">{{ playerState.currentSong.title }}</h1>
                <p v-if="lyricArtist" class="player-page__song-artist">{{ lyricArtist }}</p>
              </div>

              <!-- 大封面（椒盐：铺满上部） -->
              <div class="player-page__cover-hero">
                <img
                  v-if="displayCoverSrc"
                  class="player-page__cover-hero-img"
                  :src="displayCoverSrc"
                  alt="歌曲封面"
                />
                <div v-else class="player-page__cover-hero-img player-page__cover-hero-placeholder">♪</div>
              </div>

              <!-- 五行歌词窗口（AMLL 式连续滚动：当前行居中，切行整体上移） -->
              <div v-if="displayedWindow.length > 0" class="player-page__song-meta">
                <div ref="metaScrollEl" class="player-page__meta-window">
                  <motion.p
                    v-for="row in displayedWindow"
                    :key="row.key"
                    class="player-page__meta-line"
                    :class="{ 'player-page__meta-current': row.isCurrent }"
                    :animate="{
                      opacity: row.isCurrent ? 1 : 0.55,
                      scale: row.isCurrent ? 1.05 : 0.92,
                      filter: row.isCurrent ? 'blur(0px)' : 'blur(0.6px)',
                    }"
                    :transition="{ type: 'spring', stiffness: 240, damping: 26 }"
                  >
                    {{ row.text }}
                  </motion.p>
                </div>
              </div>

              <!-- 手机控件区（平板移入底部控制条后隐藏） -->
              <div class="player-page__info-controls">
              <div
                class="player-page__progress-area"
                @touchstart.stop="onProgressGestureStart"
                @touchmove.stop
                @touchend.stop="onProgressGestureEnd"
                @touchcancel.stop="onProgressGestureEnd"
                @pointerdown.stop="onProgressGestureStart"
                @pointerup.stop="onProgressGestureEnd"
                @pointercancel.stop="onProgressGestureEnd"
              >
                <m-range
                  ref="progressRangeRef"
                  class="progress-range player-page__progress-range"
                  :min="0"
                  :max="durationForSlider"
                  :step="0.1"
                  :value="effectiveSeekPosition"
                  :disabled="!canSeek"
                  aria-label="播放进度"
                  @input="onRangeInput"
                  @change="onRangeChange"
                />
                <div class="player-page__time-row">
                  <span>{{ formatTime(playerState.position) }}</span>
                  <span v-if="bufferHintVisible" class="player-page__buffer-hint">缓冲中</span>
                  <span>{{ playerState.duration ? formatTime(playerState.duration) : '--:--' }}</span>
                </div>
              </div>

              <div class="controls player-page__controls">
                <m-icon-button
                  size="lg"
                  class="player-page__side-btn"
                  aria-label="上一曲"
                  @click="onPrevious"
                >
                  <component :is="previousIcon" aria-hidden="true" class="player-page__icon-lg" />
                </m-icon-button>
                <m-icon-button
                  size="lg"
                  class="player-page__play-btn"
                  :disabled="playerState.status === 'loading'"
                  aria-label="播放或暂停"
                  @click="togglePlayback"
                >
                  <component :is="isPlaying ? pause : play" aria-hidden="true" class="player-page__icon-lg" />
                </m-icon-button>
                <m-icon-button
                  size="lg"
                  class="player-page__side-btn"
                  aria-label="下一曲"
                  @click="onNext"
                >
                  <component :is="nextIcon" aria-hidden="true" class="player-page__icon-lg" />
                </m-icon-button>
              </div>

              <div class="mode-bar player-page__mode-bar">
                <m-icon-button
                  class="player-page__mode-btn"
                  :aria-label="repeatModeLabel"
                  @click="onToggleRepeat"
                >
                  <component :is="repeatIcon" aria-hidden="true" class="player-page__icon" />
                </m-icon-button>

                <m-icon-button
                  class="player-page__mode-btn"
                  :aria-label="shuffleModeLabel"
                  @click="onToggleShuffle"
                >
                  <component :is="shuffleIcon" aria-hidden="true" class="player-page__icon" />
                </m-icon-button>

                <m-icon-button
                  class="player-page__mode-btn"
                  aria-label="播放队列"
                  @click="goToQueue"
                >
                  <component :is="listIcon" aria-hidden="true" class="player-page__icon" />
                </m-icon-button>

                <m-icon-button
                  class="player-page__mode-btn"
                  aria-label="更多"
                  @click="openPlayerActions"
                >
                  <component :is="moreIcon" aria-hidden="true" class="player-page__icon" />
                </m-icon-button>
              </div>
              </div>
            </div>
          </section>

          <section
            ref="lyricPanelRef"
            class="panel lyric-panel player-page__lyric-panel"
            aria-label="歌词页"
            @pointerup="onLyricPanelPointerUp"
          >
            <header v-if="playerState.currentSong" class="lyric-header player-page__lyric-header">
              <h2 class="player-page__lyric-title">{{ playerState.currentSong.title }}</h2>
              <p v-if="lyricArtist" class="player-page__lyric-artist">{{ lyricArtist }}</p>
            </header>

            <template v-if="hasLyrics">
              <LyricPlayer
                ref="lyricPlayerRef"
                :key="lyricPlayerKey"
                class="lyric-player player-page__lyric-player"
                :data-translation-visible="showLyricTranslation ? 'true' : 'false'"
                :lyric-lines="displayLyricLines"
                :current-time="lyricRenderTime"
                align-anchor="center"
                :align-position="0.5"
                :enable-spring="true"
                :enable-blur="true"
                :enable-scale="true"
                :word-fade-width="0.5"
                @line-click="onLyricLineClick"
                @wheel="onLyricUserScroll"
                @touchmove="onLyricUserScroll"
              />
            </template>
            <div v-else class="player-page__lyric-empty">
              <h2>{{ lyricEmptyTitle }}</h2>
              <p>{{ lyricEmptyDescription }}</p>
            </div>

            <motion.div
              v-if="showLyricFloatingActions"
              class="lyric-floating-actions player-page__lyric-fabs"
              :class="[
                { 'is-visible': lyricChromeVisible },
                hasLyricTranslation ? 'player-page__lyric-fabs--split' : 'player-page__lyric-fabs--end',
              ]"
              :initial="false"
              :animate="{ opacity: lyricChromeVisible ? 1 : 0 }"
              :transition="{ duration: 0.2 }"
              aria-label="歌词快捷操作"
              :aria-hidden="!lyricChromeVisible"
            >
              <m-icon-button
                v-if="hasLyricTranslation"
                class="lyric-fab player-page__lyric-fab"
                :class="{ 'is-active': showLyricTranslation }"
                :aria-label="showLyricTranslation ? '隐藏翻译' : '显示翻译'"
                :tabindex="lyricChromeVisible ? 0 : -1"
                @click.stop="onLyricTranslateClick"
              >
                <component :is="translationIcon" aria-hidden="true" class="player-page__icon" />
              </m-icon-button>

              <m-icon-button
                v-if="!isTabletLayout"
                size="lg"
                class="lyric-fab player-page__lyric-play-fab"
                :aria-label="isPlaying ? '暂停播放' : '继续播放'"
                :disabled="playerState.status === 'loading'"
                :tabindex="lyricChromeVisible ? 0 : -1"
                @click.stop="onLyricPlayClick"
              >
                <component :is="isPlaying ? pause : play" aria-hidden="true" class="player-page__icon-lg" />
              </m-icon-button>
            </motion.div>
          </section>
          </motion.div>

          <!-- 平板底部全宽控制条（仅横屏平板 ≥768px 且宽>高；手机/竖屏由 info-panel 内控件承担） -->
          <div v-if="isTabletLayout" class="player-page__bottom-bar">
            <div
              class="player-page__bottom-progress"
              @touchstart.stop="onProgressGestureStart"
              @touchmove.stop
              @touchend.stop="onProgressGestureEnd"
              @touchcancel.stop="onProgressGestureEnd"
              @pointerdown.stop="onProgressGestureStart"
              @pointerup.stop="onProgressGestureEnd"
              @pointercancel.stop="onProgressGestureEnd"
            >
              <m-range
                ref="progressRangeRef"
                class="progress-range player-page__progress-range"
                :min="0"
                :max="durationForSlider"
                :step="0.1"
                :value="effectiveSeekPosition"
                :disabled="!canSeek"
                aria-label="播放进度"
                @input="onRangeInput"
                @change="onRangeChange"
              />
              <div class="player-page__time-row">
                <span>{{ formatTime(playerState.position) }}</span>
                <span v-if="bufferHintVisible" class="player-page__buffer-hint">缓冲中</span>
                <span>{{ playerState.duration ? formatTime(playerState.duration) : '--:--' }}</span>
              </div>
            </div>

            <div class="player-page__bottom-row">
              <div class="mode-bar player-page__bottom-mode">
                <m-icon-button
                  class="player-page__mode-btn"
                  :aria-label="repeatModeLabel"
                  @click="onToggleRepeat"
                >
                  <component :is="repeatIcon" aria-hidden="true" class="player-page__icon" />
                </m-icon-button>
                <m-icon-button
                  class="player-page__mode-btn"
                  :aria-label="shuffleModeLabel"
                  @click="onToggleShuffle"
                >
                  <component :is="shuffleIcon" aria-hidden="true" class="player-page__icon" />
                </m-icon-button>
              </div>

              <div class="controls player-page__bottom-controls">
                <m-icon-button
                  size="lg"
                  class="player-page__side-btn"
                  aria-label="上一曲"
                  @click="onPrevious"
                >
                  <component :is="previousIcon" aria-hidden="true" class="player-page__icon-lg" />
                </m-icon-button>
                <m-icon-button
                  size="lg"
                  class="player-page__play-btn"
                  :disabled="playerState.status === 'loading'"
                  aria-label="播放或暂停"
                  @click="togglePlayback"
                >
                  <component :is="isPlaying ? pause : play" aria-hidden="true" class="player-page__icon-lg" />
                </m-icon-button>
                <m-icon-button
                  size="lg"
                  class="player-page__side-btn"
                  aria-label="下一曲"
                  @click="onNext"
                >
                  <component :is="nextIcon" aria-hidden="true" class="player-page__icon-lg" />
                </m-icon-button>
              </div>

              <div class="mode-bar player-page__bottom-mode">
                <m-icon-button
                  class="player-page__mode-btn"
                  aria-label="播放队列"
                  @click="goToQueue"
                >
                  <component :is="listIcon" aria-hidden="true" class="player-page__icon" />
                </m-icon-button>
                <m-icon-button
                  class="player-page__mode-btn"
                  aria-label="更多"
                  @click="openPlayerActions"
                >
                  <component :is="moreIcon" aria-hidden="true" class="player-page__icon" />
                </m-icon-button>
              </div>
            </div>
          </div>
        </template>
      </div>
    </div>

    <!-- 歌曲操作：仅「编辑歌曲信息」（D2） -->
    <m-actions :opened="isPlayerActionsOpen" @backdropclick="isPlayerActionsOpen = false">
      <m-actions-group>
        <m-actions-label>歌曲操作</m-actions-label>
        <m-actions-button @click="onOpenSongEdit">编辑歌曲信息</m-actions-button>
        <m-actions-button @click="toggleCurrentSongScrapeQueue">{{ isCurrentSongInScrapeQueue ? '取消待刮削' : '标记待刮削' }}</m-actions-button>
        <m-actions-button @click="isPlayerActionsOpen = false">取消</m-actions-button>
      </m-actions-group>
    </m-actions>

    <!-- 编辑歌曲信息：title/artist/album/封面/歌词/RG + 云端获取 -->
    <m-sheet :opened="isSongEditOpen">
      <div class="player-page__sheet-title">编辑歌曲信息</div>
      <form
        class="player-page__edit-form"
        @submit.prevent="editForm.handleSubmit"
      >
        <!-- tab：基础信息 / 歌词（渠道拆分） -->
        <div class="player-page__seg">
          <button
            type="button"
            class="player-page__seg-btn"
            :class="{ 'player-page__seg-btn--active': editMetaTab === 'basic' }"
            :aria-pressed="editMetaTab === 'basic'"
            @click="editMetaTab = 'basic'"
          >
            基础信息
          </button>
          <button
            type="button"
            class="player-page__seg-btn"
            :class="{ 'player-page__seg-btn--active': editMetaTab === 'lyrics' }"
            :aria-pressed="editMetaTab === 'lyrics'"
            @click="editMetaTab = 'lyrics'"
          >
            歌词
          </button>
        </div>

        <template v-if="editMetaTab === 'basic'">
          <!-- 云端强制搜：预览 + 勾选应用，不自动覆盖表单 -->
          <section
            class="player-page__cloud-card"
            aria-label="从云端获取元信息"
          >
            <div class="player-page__cloud-field">
              <p class="player-page__label">来源平台</p>
              <div class="player-page__pill-row">
                <button
                  v-for="item in cloudPlatforms"
                  :key="item.id"
                  type="button"
                  class="player-page__pill"
                  :class="{ 'player-page__pill--active': cloudPlatform === item.id }"
                  :aria-pressed="cloudPlatform === item.id"
                  :disabled="cloudFetching || cloudApplying"
                  @click="cloudPlatform = item.id"
                >
                  {{ item.label }}
                </button>
              </div>
            </div>

            <div class="player-page__row">
              <p class="player-page__label">云端元信息</p>
              <m-button
                component="button"
                variant="clear"
                size="small"
                rounded
                :disabled="isEditSubmitting || cloudFetching || cloudApplying"
                aria-label="从云端获取标题艺人专辑封面与歌词"
                @click="onFetchCloudMeta"
              >
                {{ cloudFetching ? '获取中…' : '从云端获取' }}
              </m-button>
            </div>

            <p
              v-if="cloudStatusMessage"
              class="player-page__muted"
            >
              {{ cloudStatusMessage }}
            </p>

            <template v-if="cloudResult">
              <div class="player-page__cloud-result">
                <p class="player-page__muted">
                  文本 · {{ dimStatusLabel(cloudResult.text.status) }}
                  <template v-if="selectedTextHit">
                    ：{{ selectedTextHit.title || '—' }} / {{ selectedTextHit.artist || '—' }} / {{ selectedTextHit.album || '—' }}
                    <span v-if="selectedTextHit.source">（{{ cloudSourceLabel(selectedTextHit.source) }}）</span>
                  </template>
                </p>
                <m-button
                  v-if="cloudResult.text.items.length > 1"
                  component="button"
                  variant="clear"
                  rounded
                  size="small"
                  class="player-page__self-start"
                  :disabled="cloudFetching || cloudApplying"
                  aria-label="更换文本候选"
                  @click="cloudExpandText = !cloudExpandText"
                >
                  {{ cloudExpandText ? '收起文本候选' : '更换文本' }}
                </m-button>
                <div v-if="cloudExpandText" class="player-page__cand-list">
                  <button
                    v-for="(item, idx) in cloudResult.text.items"
                    :key="`text-${idx}-${item.source}`"
                    type="button"
                    class="player-page__cand-btn"
                    :class="{ 'player-page__cand-btn--selected': idx === cloudTextIndex }"
                    @click="cloudTextIndex = idx"
                  >
                    {{ item.title || '—' }} · {{ item.artist || '—' }} · {{ item.album || '—' }}
                    <span class="player-page__cand-source">（{{ cloudSourceLabel(item.source) }}）</span>
                  </button>
                </div>

                <p class="player-page__muted">
                  封面 · {{ dimStatusLabel(cloudResult.cover.status) }}
                  <template v-if="selectedCoverHit">（{{ cloudSourceLabel(selectedCoverHit.source) }}）</template>
                </p>
                <img
                  v-if="selectedCoverHit"
                  class="player-page__cover-thumb"
                  :src="selectedCoverHit.remoteUrl"
                  alt="云端封面预览"
                >
                <m-button
                  v-if="cloudResult.cover.items.length > 1"
                  component="button"
                  variant="clear"
                  rounded
                  size="small"
                  class="player-page__self-start"
                  :disabled="cloudFetching || cloudApplying"
                  aria-label="更换封面候选"
                  @click="cloudExpandCover = !cloudExpandCover"
                >
                  {{ cloudExpandCover ? '收起封面候选' : '更换封面' }}
                </m-button>
                <div v-if="cloudExpandCover" class="player-page__cover-cands">
                  <button
                    v-for="(item, idx) in cloudResult.cover.items"
                    :key="`cover-${idx}-${item.source}`"
                    type="button"
                    class="player-page__cover-cand"
                    :class="{ 'player-page__cover-cand--selected': idx === cloudCoverIndex }"
                    :aria-label="`封面候选 ${idx + 1} ${cloudSourceLabel(item.source)}`"
                    @click="cloudCoverIndex = idx"
                  >
                    <img class="player-page__cover-cand-img" :src="item.remoteUrl" alt="">
                  </button>
                </div>
              </div>

              <div class="player-page__cloud-apply">
                <p class="player-page__label">应用到表单的字段</p>
                <div class="player-page__check-row">
                  <label class="player-page__check-label">
                    <m-checkbox
                      :checked="cloudChecks.title"
                      :disabled="!selectedTextHit?.title?.trim() || cloudFetching || cloudApplying"
                      aria-label="应用标题"
                      @change="onCloudCheckChange('title', $event)"
                    />
                    标题
                  </label>
                  <label class="player-page__check-label">
                    <m-checkbox
                      :checked="cloudChecks.artist"
                      :disabled="!selectedTextHit?.artist?.trim() || cloudFetching || cloudApplying"
                      aria-label="应用艺术家"
                      @change="onCloudCheckChange('artist', $event)"
                    />
                    艺术家
                  </label>
                  <label class="player-page__check-label">
                    <m-checkbox
                      :checked="cloudChecks.album"
                      :disabled="!selectedTextHit?.album?.trim() || cloudFetching || cloudApplying"
                      aria-label="应用专辑"
                      @change="onCloudCheckChange('album', $event)"
                    />
                    专辑
                  </label>
                  <label class="player-page__check-label">
                    <m-checkbox
                      :checked="cloudChecks.cover"
                      :disabled="!selectedCoverHit || cloudFetching || cloudApplying"
                      aria-label="应用封面"
                      @change="onCloudCheckChange('cover', $event)"
                    />
                    封面
                  </label>
                </div>
                <m-button
                  component="button"
                  size="small"
                  rounded
                  class="player-page__self-start"
                  :disabled="!canApplyCloud || cloudFetching || cloudApplying || isEditSubmitting"
                  aria-label="将勾选的云端字段应用到表单"
                  @click="onApplyCloudMeta"
                >
                  {{ cloudApplying ? '应用中…' : '应用到表单' }}
                </m-button>
              </div>
            </template>
          </section>

          <editForm.Field
            name="title"
            :validators="{
              onSubmit: ({ value }) => (String(value ?? '').trim() ? undefined : '请填写歌曲标题'),
            }"
          >
            <template #default="{ field }">
              <m-list-input
                label="标题"
                :error="typeof field.state.meta.errors[0] === 'string' ? field.state.meta.errors[0] : undefined"
                :disabled="isEditSubmitting"
              >
                <template #input>
                  <input
                    :value="field.state.value"
                    type="text"
                    placeholder="标题"
                    class="player-page__field-input"
                    @input="(e: Event) => field.handleChange((e.target as HTMLInputElement).value)"
                    @blur="field.handleBlur"
                  />
                </template>
              </m-list-input>
            </template>
          </editForm.Field>

          <editForm.Field name="artist">
            <template #default="{ field }">
              <m-list-input
                label="艺术家"
                :disabled="isEditSubmitting"
              >
                <template #input>
                  <input
                    :value="field.state.value"
                    type="text"
                    placeholder="艺术家"
                    class="player-page__field-input"
                    @input="(e: Event) => field.handleChange((e.target as HTMLInputElement).value)"
                    @blur="field.handleBlur"
                  />
                </template>
              </m-list-input>
            </template>
          </editForm.Field>

          <editForm.Field name="album">
            <template #default="{ field }">
              <m-list-input
                label="专辑"
                :disabled="isEditSubmitting"
              >
                <template #input>
                  <input
                    :value="field.state.value"
                    type="text"
                    placeholder="专辑"
                    class="player-page__field-input"
                    @input="(e: Event) => field.handleChange((e.target as HTMLInputElement).value)"
                    @blur="field.handleBlur"
                  />
                </template>
              </m-list-input>
            </template>
          </editForm.Field>

          <div class="player-page__cover-edit">
            <p class="player-page__label">封面</p>
            <div class="player-page__cover-edit-row">
              <img
                v-if="editCoverPreviewSrc"
                class="player-page__cover-edit-preview"
                :src="editCoverPreviewSrc"
                alt="封面预览"
              >
              <div
                v-else
                class="player-page__cover-edit-placeholder"
                aria-hidden="true"
              >
                ♪
              </div>
              <div class="player-page__cover-edit-actions">
                <m-button
                  component="button"
                  variant="clear"
                  size="small"
                  rounded
                  :disabled="isEditSubmitting"
                  @click="onPickCover"
                >
                  选择图片
                </m-button>
                <m-button
                  v-if="editCoverPreviewSrc"
                  component="button"
                  variant="clear"
                  size="small"
                  :disabled="isEditSubmitting"
                  @click="onClearCover"
                >
                  清除封面
                </m-button>
              </div>
            </div>
            <input
              ref="coverFileInputRef"
              class="player-page__hidden-input"
              type="file"
              accept="image/*"
              @change="onCoverFileChange"
            >
          </div>

          <editForm.Field
            name="replayGainDb"
            :validators="{
              onSubmit: ({ value }) => validateReplayGainInput(String(value ?? '')),
            }"
          >
            <template #default="{ field }">
              <m-list-input
                label="音量均衡（ReplayGain dB）"
                :error="typeof field.state.meta.errors[0] === 'string' ? field.state.meta.errors[0] : undefined"
                :disabled="isEditSubmitting"
              >
                <template #input>
                  <input
                    :value="field.state.value"
                    type="text"
                    placeholder="如 -6.5，空=清除"
                    class="player-page__field-input"
                    @input="(e: Event) => field.handleChange((e.target as HTMLInputElement).value)"
                    @blur="field.handleBlur"
                  />
                </template>
              </m-list-input>
            </template>
          </editForm.Field>
        </template>

        <template v-else>
          <!-- 云端歌词：独立渠道 + 独立来源选择 -->
          <section
            class="player-page__cloud-card"
            aria-label="从云端获取歌词"
          >
            <div class="player-page__cloud-field">
              <p class="player-page__label">歌词来源</p>
              <div class="player-page__pill-row">
                <button
                  v-for="item in cloudLyricsPlatforms"
                  :key="item.id"
                  type="button"
                  class="player-page__pill"
                  :class="{ 'player-page__pill--active': lyricsPlatform === item.id }"
                  :aria-pressed="lyricsPlatform === item.id"
                  :disabled="cloudFetching || cloudApplying"
                  @click="lyricsPlatform = item.id"
                >
                  {{ item.label }}
                </button>
              </div>
            </div>

            <div class="player-page__row">
              <p class="player-page__label">云端歌词</p>
              <m-button
                component="button"
                variant="clear"
                size="small"
                rounded
                :disabled="isEditSubmitting || cloudFetching || cloudApplying"
                aria-label="从云端获取歌词"
                @click="onFetchCloudMeta"
              >
                {{ cloudFetching ? '获取中…' : '获取歌词' }}
              </m-button>
            </div>

            <p
              v-if="lyricsStatusMessage"
              class="player-page__muted"
            >
              {{ lyricsStatusMessage }}
            </p>

            <template v-if="cloudResult?.lyrics?.items?.length">
              <p class="player-page__muted">
                候选 · {{ cloudResult.lyrics.items.length }} 条
              </p>
              <div class="player-page__cand-list">
                <button
                  v-for="(item, idx) in cloudResult.lyrics.items"
                  :key="`lyrics-${idx}-${item.source}`"
                  type="button"
                  class="player-page__cand-btn"
                  :class="{ 'player-page__cand-btn--selected': idx === cloudLyricsIndex }"
                  @click="cloudLyricsIndex = idx"
                >
                  {{ cloudSourceLabel(item.source) }} · {{ item.format }}
                  <span class="player-page__cand-source">{{ lyricsPreview(item.text, 40) }}</span>
                </button>
              </div>
              <p
                v-if="selectedLyricsHit"
                class="player-page__lyrics-preview"
              >
                {{ selectedLyricsHit.text }}
              </p>
              <m-button
                component="button"
                size="small"
                rounded
                class="player-page__self-start"
                :disabled="!selectedLyricsHit || cloudFetching || cloudApplying || isEditSubmitting"
                aria-label="将所选云端歌词应用到歌词输入框"
                @click="onApplyLyrics"
              >
                {{ cloudApplying ? '应用中…' : '应用所选歌词' }}
              </m-button>
            </template>
          </section>

          <editForm.Field name="lyrics">
            <template #default="{ field }">
              <m-list-input
                label="歌词文本"
                :disabled="isEditSubmitting"
              >
                <template #input>
                  <textarea
                    :value="field.state.value"
                    rows="4"
                    placeholder="歌词文本"
                    class="player-page__lyrics-input"
                    @input="(e: Event) => onEditLyricsInput(field, (e.target as HTMLTextAreaElement).value)"
                    @blur="field.handleBlur"
                  ></textarea>
                </template>
              </m-list-input>
            </template>
          </editForm.Field>
        </template>

        <p v-if="editFormError" class="player-page__error-text">
          {{ editFormError }}
        </p>

        <div class="player-page__form-actions">
          <m-button component="button" variant="clear" rounded :disabled="isEditSubmitting" @click="closeSongEdit">
            取消
          </m-button>
          <m-button component="button" type="submit" rounded :disabled="isEditSubmitting">
            {{ isEditSubmitting ? '保存中…' : '保存' }}
          </m-button>
        </div>
      </form>
    </m-sheet>

    <m-toast :opened="toast.visible" position="center">
      {{ toast.message }}
    </m-toast>
  </m-popup>
</template>

<script setup lang="ts">
import {
  MActions,
  MActionsButton,
  MActionsGroup,
  MActionsLabel,
  MButton,
  MCheckbox,
  MIconButton,
  MListInput,
  MPopup,
  MRange,
  MSheet,
  MToast,
} from '@/components/ui'
import { animate, motion, type AnimationPlaybackControls } from 'motion-v'
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useForm } from '@tanstack/vue-form'
import { Capacitor } from '@capacitor/core'
import {
  searchEditCloudMeta,
  type CloudLyricsPlatformId,
  type CloudPlatformId,
  type EditCloudMetaResult,
  type EditDimKey,
  type EditDimResult,
  type EditDimStatus,
  type SearchEditCloudMetaOptions,
} from '@/features/editMeta'
import { cacheRemoteCover } from '@/features/player/native'
import {
  ellipsisVertical,
  languageOffOutline,
  languageOutline,
  list,
  listOutline,
  pause,
  play,
  playSkipBack,
  playSkipForward,
  repeat,
  repeatOutline,
  shuffle,
} from '@/icons'
import { BackgroundRender, LyricPlayer } from '@applemusic-like-lyrics/vue'
import { MeshGradientRenderer } from '@applemusic-like-lyrics/core'
import type { LyricLine, LyricLineMouseEvent } from '@applemusic-like-lyrics/core'
import { parseLrc, parseQrc, parseTTML, parseYrc } from '@applemusic-like-lyrics/lyric'
import '@applemusic-like-lyrics/core/style.css'
import { applyLyricTranslationVisibility } from '@/features/lyrics/display'
import { prepareLyricLinesForDisplay } from '@/features/lyrics/mergeTranslation'
import { loadSongs } from '@/features/library/storage'
import { cacheCoverBytes } from '@/features/library/native'
import { enqueueScrapeSongs, isInScrapeQueue, removeScrapeSongs, onScrapeQueueChanged } from '@/features/scrape/queue'
import type { SongLyricsFormat } from '@/features/library/types'
import {
  isPlaying,
  pausePlayback,
  playerState,
  playNextFromQueue,
  playPreviousFromQueue,
  queueState,
  resumePlayback,
  saveCurrentSongUserEdit,
  seekPlayback,
  setRepeatMode,
  toggleShuffle,
} from '@/features/player/controller'
import { closePlayerOverlay, openQueueOverlay, playerOverlayVisible } from '@/features/player/overlay'

const activePanel = ref(0)
/** 手势落点判断用的 template ref（取代 closest / class 选择器查询）。 */
const lyricPanelRef = ref<HTMLElement | null>(null)
const lyricPlayerRef = ref<HTMLElement | { $el?: HTMLElement } | null>(null)

/** 歌词滚动后自动回高亮行：停止滚动 2 秒后调 AMLL resetScroll + calcLayout */
let lyricScrollBackTimer: ReturnType<typeof setTimeout> | null = null
const getLyricPlayerInstance = ():
  | { resetScroll(): void; calcLayout(sync?: boolean, force?: boolean): Promise<void> }
  | null => {
  // expose() 返回对象经 proxyRefs 自动解包：lyricPlayer 直接是 player 实例（非 Ref，无 .value）
  const comp = lyricPlayerRef.value as unknown as {
    lyricPlayer?: { resetScroll(): void; calcLayout(sync?: boolean, force?: boolean): Promise<void> }
  } | null
  return comp?.lyricPlayer ?? null
}
const onLyricUserScroll = (): void => {
  if (lyricScrollBackTimer) clearTimeout(lyricScrollBackTimer)
  lyricScrollBackTimer = setTimeout(() => {
    lyricScrollBackTimer = null
    // AMLL 自带 5 秒归位（仅播放中时间更新时生效）；此处 2 秒主动归位，暂停时也生效
    const player = getLyricPlayerInstance()
    if (player) {
      player.resetScroll()
      void player.calcLayout()
    }
  }, 2000)
}
/** 拖拽位移绑定元素（内层 drag-layer）。拖拽跟手由 Vue :style 写 transform，
 * 回弹动画也作用在同一元素，避免旧实现「内层先归 0、外层再动画」引起的松手抖动与中途残留。 */
const dragLayerRef = ref<HTMLElement | null>(null)
/** 松手回弹动画句柄（motion 命令式）；拖拽跟手由状态驱动（无过渡） */
let reboundControls: AnimationPlaybackControls | null = null
const stopRebound = (): void => {
  if (reboundControls) {
    // motion stop() = commitStyles + cancel（不触发 onComplete）：
    // transform 停留在当前中间值，由调用方紧接着锁回起点或兜底清零，不留无人纠正的残留。
    reboundControls.stop()
    reboundControls = null
  }
}
const progressRangeRef = ref<HTMLElement | { $el?: HTMLElement } | null>(null)
/** 隐藏态冻结传给 AMLL 的时间输入；重新打开时由当前播放进度立即刷新。 */
const hiddenLyricTime = ref(0)
/**
 * 传给 AMLL 的当前时间（毫秒）。
 * 播完/暂停在末尾时 position 会超出最后一句歌词的结束时间，AMLL 找不到活动行导致全部歌词失活模糊；
 * 钳制上限到最后一句 endTime，让最后一行保持完成高亮（对齐主流播放器行为）。
 */
const lyricRenderTime = computed(() => {
  const ms = playerOverlayVisible.value ? playerState.position * 1000 : hiddenLyricTime.value
  const lines = lyricLines.value
  if (lines.length > 0) {
    const last = lines[lines.length - 1]
    const lastEnd = Number.isFinite(last.endTime) && last.endTime > 0 ? last.endTime : last.startTime
    return Math.min(ms, Math.max(lastEnd, 0))
  }
  return ms
})
const touchStartX = ref<number | null>(null)
const touchStartY = ref<number | null>(null)
const dragOffsetY = ref(0)
const gestureDirection = ref<'horizontal' | 'vertical' | null>(null)

/** 显式回弹：终止旧动画 → 锁回拖拽终点 → 0.22s easeOut 动画回顶（全量 Motion 命令式） */
const startRebound = (from: number): void => {
  stopRebound()
  const el = dragLayerRef.value
  if (!el || from <= 0) {
    return
  }
  // 锁回起点（覆盖旧动画 commit 的中间值；也为「Vue 已 patch 成 0」锁回正确动画起点）
  el.style.transform = `translateY(${from}px)`
  reboundControls = animate(
    el,
    { transform: 'translateY(0px)' },
    {
      duration: 0.22,
      ease: 'easeOut',
      onComplete: () => {
        // 动画完成：确保内联 transform 与目标态一致（防 fill 行为差异残留中间值）
        el.style.transform = 'translateY(0px)'
        reboundControls = null
      },
    },
  )
}
/** 终止回弹并立即清零位移（不播回弹动画）：用于触摸会话重建/打断兜底、
 * 进度条与歌词点击等非拖拽手势上下文、打开/关闭播放页的状态重置。 */
const clearDragOffsetImmediate = (): void => {
  stopRebound()
  if (dragLayerRef.value) {
    dragLayerRef.value.style.transform = 'translateY(0px)'
  }
  dragOffsetY.value = 0
}
const isDraggingVertically = ref(false)
const canDragDown = ref(false)
const showLyricTranslation = ref(true)
/** 歌词页浮动 chrome：默认隐藏，交互后显示，空闲 3s 再藏。 */
const lyricChromeVisible = ref(false)
let lyricChromeIdleTimer: ReturnType<typeof setTimeout> | null = null
const LYRIC_FAB_IDLE_MS = 3000
const viewportWidth = ref(typeof window === 'undefined' ? 0 : window.innerWidth)
const viewportHeight = ref(typeof window === 'undefined' ? 0 : window.innerHeight)
/** 进度条交互中或结束后的短保护期，防止松手穿透到上一曲/下一曲或横向切面板。 */
const seekGestureLocked = ref(false)
let seekUnlockTimer: ReturnType<typeof setTimeout> | null = null
const SEEK_CLICK_GUARD_MS = 300
const meshGradientRenderer = MeshGradientRenderer

const repeatModeLabel = computed(() => queueState.repeatMode === 'one' ? '单曲循环' : '列表循环')
const repeatIcon = computed(() => queueState.repeatMode === 'one' ? repeat : repeatOutline)
const shuffleModeLabel = computed(() => queueState.shuffleEnabled ? '随机播放' : '顺序播放')
const shuffleIcon = computed(() => queueState.shuffleEnabled ? shuffle : listOutline)
const listIcon = list
const moreIcon = ellipsisVertical
const translationIcon = computed(() => showLyricTranslation.value ? languageOutline : languageOffOutline)
const previousIcon = playSkipBack
const nextIcon = playSkipForward
/** 横屏平板（≥768px 且宽>高）才启用双栏 + 底部控制条；竖屏（含竖屏平板）保持手机式全屏沉浸。 */
const isTabletLayout = computed(
  () => viewportWidth.value >= 768 && viewportHeight.value < viewportWidth.value,
)

// ── 更多 / 编辑歌曲信息 ──────────────────────────────────────────
const isPlayerActionsOpen = ref(false)
const isSongEditOpen = ref(false)

// ── child2：待刮削队列标记 ───────────────────────
const isCurrentSongInScrapeQueue = ref(false)
const refreshCurrentSongScrapeState = (): void => {
  const id = playerState.currentSong?.id
  isCurrentSongInScrapeQueue.value = id ? isInScrapeQueue(id) : false
}
const toggleCurrentSongScrapeQueue = (): void => {
  const song = playerState.currentSong
  if (!song) {
    return
  }
  if (isCurrentSongInScrapeQueue.value) {
    removeScrapeSongs([song.id])
    showToast('已取消待刮削', 'default')
  } else {
    enqueueScrapeSongs([song.id])
    showToast('已加入待刮削队列', 'success')
  }
  isPlayerActionsOpen.value = false
}
const unsubscribeScrapeQueue = onScrapeQueueChanged(() => {
  refreshCurrentSongScrapeState()
})
const editFormError = ref('')
const coverFileInputRef = ref<HTMLInputElement | null>(null)
/** 编辑中的安全封面 URI；null + cleared 表示用户清除 */
const editCoverUri = ref<string | null>(null)
const editCoverCleared = ref(false)
/** 仅用户改过封面时才写入 patch，避免误标 userEditedFields.cover */
const editCoverDirty = ref(false)
/** 应用歌词时的 format；保存 dirty 时写入 patch；手改文本回落 lrc */
const editLyricsFormat = ref<SongLyricsFormat | null>(null)
/** 打开编辑时的基线，保存时仅提交相对基线有变化的字段 */
const editBaseline = ref({
  title: '',
  artist: '',
  album: '',
  lyrics: '',
  replayGainDb: '',
})
const editCoverPreviewSrc = computed(() => {
  if (editCoverCleared.value) {
    return ''
  }
  const uri = editCoverUri.value || ''
  return uri ? toDisplayableUri(uri) : ''
})

const toast = ref<{
  visible: boolean
  message: string
  variant: 'default' | 'success' | 'warning' | 'danger'
  duration: number
}>({
  visible: false,
  message: '',
  variant: 'default',
  duration: 2200,
})

let toastTimer: number | undefined

const showToast = (
  message: string,
  variant: 'default' | 'success' | 'warning' | 'danger' = 'default',
  duration = 2200,
): void => {
  toast.value = { visible: true, message, variant, duration }
  window.clearTimeout(toastTimer)
  toastTimer = window.setTimeout(() => {
    toast.value.visible = false
  }, duration)
}

/** k-range 的原生 input/change 事件适配 */
const onRangeInput = (e: Event): void => {
  onSeekInput(Number((e.target as HTMLInputElement).value))
}

const onRangeChange = (e: Event): void => {
  void onSeek(Number((e.target as HTMLInputElement).value))
}

/** k-checkbox 的原生 change 事件适配 cloudChecks */
const onCloudCheckChange = (key: keyof typeof cloudChecks.value, e: Event): void => {
  cloudChecks.value[key] = (e.target as HTMLInputElement).checked
}

const validateReplayGainInput = (value: string): string | undefined => {
  const trimmed = value.trim()
  if (!trimmed) {
    return undefined
  }
  const normalized = trimmed.replace(/\s*dB\s*$/i, '').trim()
  const num = Number(normalized)
  if (!Number.isFinite(num)) {
    return '请输入合法的 dB 数值'
  }
  if (Math.abs(num) > 30) {
    return 'ReplayGain 应在 ±30 dB 内'
  }
  return undefined
}

const parseReplayGainInput = (value: string): number | null => {
  const trimmed = value.trim()
  if (!trimmed) {
    return null
  }
  const normalized = trimmed.replace(/\s*dB\s*$/i, '').trim()
  const num = Number(normalized)
  return Number.isFinite(num) ? num : null
}

const editForm = useForm({
  defaultValues: {
    title: '',
    artist: '',
    album: '',
    lyrics: '',
    replayGainDb: '',
  },
  onSubmit: async ({ value }) => {
    editFormError.value = ''
    const songId = playerState.currentSong?.id
    if (!songId) {
      editFormError.value = '当前没有播放中的歌曲。'
      return
    }

    const title = value.title.trim()
    if (!title) {
      editFormError.value = '请填写歌曲标题'
      return
    }

    const rgRaw = value.replayGainDb.trim()
    const rgError = validateReplayGainInput(rgRaw)
    if (rgError) {
      editFormError.value = rgError
      return
    }

    const baseline = editBaseline.value
    const artist = value.artist.trim()
    const album = value.album.trim()
    const lyrics = value.lyrics
    const nextRg = parseReplayGainInput(rgRaw)
    const baseRg = parseReplayGainInput(baseline.replayGainDb)

    // 仅提交相对打开时基线有变化的字段，避免「只改 title 也永久锁死 lyrics/RG」
    const patch: Parameters<typeof saveCurrentSongUserEdit>[0] = {}
    if (title !== baseline.title.trim()) {
      patch.title = title
    }
    if (artist !== baseline.artist.trim()) {
      patch.artist = artist
    }
    if (album !== baseline.album.trim()) {
      patch.album = album
    }
    if (lyrics !== baseline.lyrics) {
      patch.lyrics = lyrics
      // 云端应用可带 ttml/yrc/qrc；手改文本默认 lrc
      patch.lyricsFormat = editLyricsFormat.value || 'lrc'
    }
    if (nextRg !== baseRg) {
      patch.replayGainTrackDb = nextRg
    }
    if (editCoverDirty.value) {
      patch.coverUri = editCoverCleared.value ? null : (editCoverUri.value || null)
    }

    if (Object.keys(patch).length === 0) {
      isSongEditOpen.value = false
      showToast('没有需要保存的修改', 'default')
      return
    }

    // title 必填：若本次未改 title 但基线为空，仍须带上非空 title 才能过库校验；否则只传变更字段
    if (patch.title === undefined && !baseline.title.trim()) {
      patch.title = title
    }

    const result = await saveCurrentSongUserEdit(patch)

    if (!result.libraryOk) {
      editFormError.value = result.fileError || '保存失败'
      showToast(result.fileError || '保存失败', 'danger')
      return
    }

    isSongEditOpen.value = false
    if (result.fileOk) {
      showToast('已保存', 'success')
    } else {
      const reason = result.fileError ? `：${result.fileError}` : ''
      showToast(`已更新曲库，写入音频文件失败${reason}`, 'warning', 3200)
    }
  },
})

const isEditSubmitting = editForm.useSelector((state) => state.isSubmitting)

const openPlayerActions = () => {
  if (!playerState.currentSong) {
    return
  }
  isPlayerActionsOpen.value = true
}

const onOpenSongEdit = () => {
  isPlayerActionsOpen.value = false
  window.setTimeout(() => {
    seedSongEditForm()
    isSongEditOpen.value = true
  }, 180)
}

const seedSongEditForm = () => {
  const current = playerState.currentSong
  if (!current) {
    return
  }
  const latest = loadSongs().find((item) => item.id === current.id)
  const title = latest?.title ?? current.title ?? ''
  const artist = latest?.artist ?? current.artist ?? ''
  const album = latest?.album ?? current.album ?? ''
  const lyrics = latest?.lyrics ?? playerState.lyrics ?? current.lyrics ?? ''
  const rg = latest?.replayGainTrackDb
  const replayGainDb = rg !== undefined && Number.isFinite(rg) ? String(rg) : ''
  const seed = {
    title,
    artist: artist || '',
    album: album || '',
    lyrics: lyrics || '',
    replayGainDb,
  }
  editForm.reset(seed)
  editBaseline.value = { ...seed }
  editCoverUri.value = latest?.coverUri || current.coverUri || playerState.coverUri || null
  editCoverCleared.value = false
  editCoverDirty.value = false
  editLyricsFormat.value = (latest?.lyricsFormat as SongLyricsFormat | undefined)
    || (playerState.lyricsFormat as SongLyricsFormat | null)
    || null
  editFormError.value = ''
  resetCloudMetaState({ abort: true })
}

const closeSongEdit = () => {
  if (isEditSubmitting.value) {
    return
  }
  abortCloudFetch()
  isSongEditOpen.value = false
}

// ── 云端元信息（tab 拆分：基础信息=文本+封面 / 歌词独立渠道）────────
/** 编辑弹窗 tab */
const editMetaTab = ref<'basic' | 'lyrics'>('basic')
/** 基础信息来源平台（MusicTag 式）；默认全部 */
const cloudPlatform = ref<CloudPlatformId>('all')
const cloudPlatforms: Array<{ id: CloudPlatformId; label: string }> = [
  { id: 'all', label: '全部' },
  { id: 'wy', label: '网易云' },
  { id: 'tx', label: 'QQ音乐' },
  { id: 'kg', label: '酷狗' },
  { id: 'kw', label: '酷我' },
  { id: 'mg', label: '咪咕' },
  { id: 'itunes', label: 'iTunes' },
]
/** 歌词来源平台（独立渠道：无 iTunes，有 LRCLIB） */
const lyricsPlatform = ref<CloudLyricsPlatformId>('all')
const cloudLyricsPlatforms: Array<{ id: CloudLyricsPlatformId; label: string }> = [
  { id: 'all', label: '全部' },
  { id: 'wy', label: '网易云' },
  { id: 'tx', label: 'QQ音乐' },
  { id: 'kg', label: '酷狗' },
  { id: 'kw', label: '酷我' },
  { id: 'mg', label: '咪咕' },
  { id: 'lrclib', label: 'LRCLIB' },
]
/** provider id → 中文平台名（候选列表展示） */
const CLOUD_SOURCE_LABELS: Record<string, string> = {
  wy: '网易云',
  tx: 'QQ音乐',
  qrc: 'QQ音乐',
  kg: '酷狗',
  kw: '酷我',
  mg: '咪咕',
  itunes: 'iTunes',
  amll: 'AMLL',
  lrclib: 'LRCLIB',
}
const cloudSourceLabel = (source: string | undefined): string =>
  (source && CLOUD_SOURCE_LABELS[source]) || source || ''
const cloudFetching = ref(false)
const cloudApplying = ref(false)
const cloudResult = ref<EditCloudMetaResult | null>(null)
const cloudStatusMessage = ref('')
const lyricsStatusMessage = ref('')
const cloudTextIndex = ref(0)
const cloudCoverIndex = ref(0)
const cloudLyricsIndex = ref(0)
const cloudExpandText = ref(false)
const cloudExpandCover = ref(false)
const cloudChecks = ref({
  title: false,
  artist: false,
  album: false,
  cover: false,
})
let cloudAbort: AbortController | null = null
let cloudFetchGeneration = 0

const abortCloudFetch = (): void => {
  cloudAbort?.abort()
  cloudAbort = null
  cloudFetchGeneration += 1
  cloudFetching.value = false
}

const resetCloudMetaState = (options?: { abort?: boolean }): void => {
  if (options?.abort) {
    abortCloudFetch()
  }
  cloudResult.value = null
  cloudStatusMessage.value = ''
  lyricsStatusMessage.value = ''
  cloudTextIndex.value = 0
  cloudCoverIndex.value = 0
  cloudLyricsIndex.value = 0
  cloudExpandText.value = false
  cloudExpandCover.value = false
  cloudChecks.value = {
    title: false,
    artist: false,
    album: false,
    cover: false,
  }
  cloudApplying.value = false
}

const dimStatusLabel = (status: EditDimStatus): string => {
  switch (status) {
    case 'ok':
      return '已命中'
    case 'network':
      return '网络异常'
    case 'aborted':
      return '已取消'
    default:
      return '无匹配'
  }
}

const lyricsPreview = (text: string, max = 80): string => {
  const oneLine = text.replace(/\s+/g, ' ').trim()
  if (oneLine.length <= max) {
    return oneLine
  }
  return `${oneLine.slice(0, max)}…`
}

const selectedTextHit = computed(() => {
  const items = cloudResult.value?.text.items
  if (!items?.length) {
    return null
  }
  return items[cloudTextIndex.value] ?? items[0] ?? null
})

const selectedCoverHit = computed(() => {
  const items = cloudResult.value?.cover.items
  if (!items?.length) {
    return null
  }
  return items[cloudCoverIndex.value] ?? items[0] ?? null
})

const selectedLyricsHit = computed(() => {
  const items = cloudResult.value?.lyrics.items
  if (!items?.length) {
    return null
  }
  return items[cloudLyricsIndex.value] ?? items[0] ?? null
})

const canApplyCloud = computed(() => {
  const c = cloudChecks.value
  if (c.title && selectedTextHit.value?.title?.trim()) {
    return true
  }
  if (c.artist && selectedTextHit.value?.artist?.trim()) {
    return true
  }
  if (c.album && selectedTextHit.value?.album?.trim()) {
    return true
  }
  if (c.cover && selectedCoverHit.value) {
    return true
  }
  return false
})

const seedCloudChecksFromSelection = (dims: EditDimKey[]): void => {
  const text = selectedTextHit.value
  const next = { ...cloudChecks.value }
  if (dims.includes('text')) {
    next.title = !!text?.title?.trim()
    next.artist = !!text?.artist?.trim()
    next.album = !!text?.album?.trim()
  }
  if (dims.includes('cover')) {
    next.cover = !!selectedCoverHit.value
  }
  cloudChecks.value = next
}

const onFetchCloudMeta = async (): Promise<void> => {
  const song = playerState.currentSong
  if (!song?.id || cloudFetching.value || isEditSubmitting.value) {
    return
  }

  const title = String(editForm.getFieldValue('title') ?? '').trim() || song.title?.trim() || ''
  if (!title) {
    const msg = '请先填写标题再获取'
    cloudStatusMessage.value = msg
    lyricsStatusMessage.value = msg
    showToast(msg, 'warning')
    return
  }

  const artist = String(editForm.getFieldValue('artist') ?? '').trim() || undefined
  const album = String(editForm.getFieldValue('album') ?? '').trim() || undefined

  const isLyricsTab = editMetaTab.value === 'lyrics'
  const dims: EditDimKey[] = isLyricsTab ? ['lyrics'] : ['text', 'cover']
  const platform = isLyricsTab ? lyricsPlatform.value : cloudPlatform.value
  const platformLabel = isLyricsTab
    ? (cloudLyricsPlatforms.find((p) => p.id === platform)?.label ?? '')
    : (cloudPlatforms.find((p) => p.id === platform)?.label ?? '')

  abortCloudFetch()
  const controller = new AbortController()
  cloudAbort = controller
  const generation = cloudFetchGeneration
  const songId = song.id
  cloudFetching.value = true
  cloudExpandText.value = false
  cloudExpandCover.value = false
  const statusMessage = platform === 'all'
    ? (isLyricsTab ? '正在从多平台获取歌词…' : '正在从多平台获取…')
    : `${isLyricsTab ? '正在从' : '正在从'}${platformLabel}${isLyricsTab ? '获取歌词…' : '获取…'}`
  if (isLyricsTab) {
    lyricsStatusMessage.value = statusMessage
  } else {
    cloudStatusMessage.value = statusMessage
  }

  try {
    const fetchOptions: SearchEditCloudMetaOptions = {
      signal: controller.signal,
      dimensions: dims,
    }
    if (isLyricsTab) {
      fetchOptions.lyricsPlatform = lyricsPlatform.value
    } else {
      fetchOptions.platform = cloudPlatform.value
    }
    const result = await searchEditCloudMeta(
      {
        songId,
        title,
        artist,
        album,
        durationSec: Number.isFinite(playerState.duration) && playerState.duration > 0
          ? playerState.duration
          : undefined,
      },
      fetchOptions,
    )

    if (
      generation !== cloudFetchGeneration
      || controller.signal.aborted
      || !isSongEditOpen.value
      || playerState.currentSong?.id !== songId
    ) {
      return
    }

    const cloudDim: Record<EditDimKey, EditDimResult<unknown>> = result

    // 合并结果：只更新本次搜索的维度，保留另一 tab 已获取内容
    const prev = cloudResult.value
    const merged: EditCloudMetaResult = {
      text: prev?.text ?? { status: 'no-match', items: [], defaultIndex: 0 },
      cover: prev?.cover ?? { status: 'no-match', items: [], defaultIndex: 0 },
      lyrics: prev?.lyrics ?? { status: 'no-match', items: [], defaultIndex: 0 },
    }
    if (dims.includes('text')) {
      merged.text = result.text
    }
    if (dims.includes('cover')) {
      merged.cover = result.cover
    }
    if (dims.includes('lyrics')) {
      merged.lyrics = result.lyrics
    }
    cloudResult.value = merged
    if (dims.includes('text')) {
      cloudTextIndex.value = result.text.defaultIndex
    }
    if (dims.includes('cover')) {
      cloudCoverIndex.value = result.cover.defaultIndex
    }
    if (dims.includes('lyrics')) {
      cloudLyricsIndex.value = result.lyrics.defaultIndex
    }
    seedCloudChecksFromSelection(dims)

    const dimLabels: Record<EditDimKey, string> = { text: '文本', cover: '封面', lyrics: '歌词' }
    const anyOk = dims.some((d) => cloudDim[d].status === 'ok')
    const anyNetwork = dims.some((d) => cloudDim[d].status === 'network')

    if (anyOk) {
      const parts = dims
        .filter((d) => cloudDim[d].status === 'ok')
        .map((d) => `${dimLabels[d]} ${cloudDim[d].items.length}`)
      const message = isLyricsTab
        ? `已获取：${parts.join(' · ')}`
        : `已获取：${parts.join(' · ')}（勾选后应用到表单）`
      if (isLyricsTab) {
        lyricsStatusMessage.value = message
      } else {
        cloudStatusMessage.value = message
      }
    } else if (anyNetwork) {
      const message = '网络异常，请稍后重试'
      if (isLyricsTab) {
        lyricsStatusMessage.value = message
      } else {
        cloudStatusMessage.value = message
      }
      showToast('云端获取网络异常', 'warning')
    } else {
      const message = '未找到匹配结果'
      if (isLyricsTab) {
        lyricsStatusMessage.value = message
      } else {
        cloudStatusMessage.value = message
      }
      showToast('未找到云端匹配', 'default')
    }
  } catch (error) {
    if (controller.signal.aborted || generation !== cloudFetchGeneration) {
      return
    }
    if (isLyricsTab) {
      lyricsStatusMessage.value = '获取失败，请重试'
    } else {
      cloudStatusMessage.value = '获取失败，请重试'
    }
    showToast('云端获取失败', 'danger')
    console.warn('[edit-cloud-meta] fetch failed', error)
  } finally {
    if (generation === cloudFetchGeneration) {
      cloudFetching.value = false
    }
    if (cloudAbort === controller) {
      cloudAbort = null
    }
  }
}

const onEditLyricsInput = (
  field: { handleChange: (value: string) => void },
  value: string | number | null | undefined,
): void => {
  field.handleChange(String(value ?? ''))
  // 手改文本默认 lrc；云端应用走 setFieldValue，不会触发本 handler
  editLyricsFormat.value = 'lrc'
}

const onApplyCloudMeta = async (): Promise<void> => {
  if (!canApplyCloud.value || cloudApplying.value || isEditSubmitting.value) {
    return
  }

  const songId = playerState.currentSong?.id
  if (!songId || !isSongEditOpen.value) {
    return
  }

  cloudApplying.value = true
  const checks = { ...cloudChecks.value }
  const text = selectedTextHit.value
  const cover = selectedCoverHit.value
  const applied: string[] = []
  const failed: string[] = []

  const stillActive = (): boolean =>
    isSongEditOpen.value && playerState.currentSong?.id === songId

  try {
    if (checks.title && text?.title?.trim()) {
      editForm.setFieldValue('title', text.title.trim())
      applied.push('标题')
    }
    if (checks.artist && text?.artist?.trim()) {
      editForm.setFieldValue('artist', text.artist.trim())
      applied.push('艺术家')
    }
    if (checks.album && text?.album?.trim()) {
      editForm.setFieldValue('album', text.album.trim())
      applied.push('专辑')
    }
    if (checks.cover && cover?.remoteUrl) {
      const localUri = await cacheRemoteCover({
        url: cover.remoteUrl,
        cacheKey: `edit-cloud-cover:${songId}:${cover.source}:${Date.now()}`,
      })
      // 关 sheet / 切歌后丢弃封面回写，避免串曲
      if (!stillActive()) {
        return
      }
      if (localUri) {
        editCoverUri.value = localUri
        editCoverCleared.value = false
        editCoverDirty.value = true
        applied.push('封面')
      } else {
        failed.push('封面')
      }
    }

    if (!stillActive()) {
      return
    }

    if (applied.length === 0 && failed.length === 0) {
      showToast('请勾选要应用的字段', 'default')
      return
    }
    if (failed.length && applied.length) {
      showToast(`已应用 ${applied.join('、')}；${failed.join('、')}失败`, 'warning', 3200)
    } else if (failed.length) {
      showToast(`${failed.join('、')}应用失败`, 'danger')
    } else {
      showToast(`已应用到表单：${applied.join('、')}`, 'success')
    }
  } finally {
    cloudApplying.value = false
  }
}

/** 歌词 tab：应用所选云端歌词到表单 */
const onApplyLyrics = (): void => {
  const lyrics = selectedLyricsHit.value
  if (!lyrics?.text?.trim() || cloudApplying.value || isEditSubmitting.value) {
    return
  }
  editForm.setFieldValue('lyrics', lyrics.text)
  const fmt = lyrics.format
  editLyricsFormat.value =
    fmt === 'ttml' || fmt === 'yrc' || fmt === 'qrc' || fmt === 'lrc' ? fmt : 'lrc'
  showToast(`已应用歌词（${cloudSourceLabel(lyrics.source)}）`, 'success')
}

watch(isSongEditOpen, (open) => {
  if (!open) {
    // 关 sheet：作废在途请求并清空未应用预览
    resetCloudMetaState({ abort: true })
  }
})

watch(
  () => playerState.currentSong?.id,
  (nextId, prevId) => {
    if (prevId && nextId !== prevId && isSongEditOpen.value) {
      resetCloudMetaState({ abort: true })
      isSongEditOpen.value = false
    }
  },
)

const onPickCover = () => {
  coverFileInputRef.value?.click()
}

const onClearCover = () => {
  editCoverUri.value = null
  editCoverCleared.value = true
  editCoverDirty.value = true
}

const onCoverFileChange = async (event: Event) => {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  // 允许重复选同一文件
  input.value = ''
  if (!file) {
    return
  }
  try {
    const buffer = await file.arrayBuffer()
    const bytes = new Uint8Array(buffer)
    if (bytes.byteLength === 0 || bytes.byteLength > 5 * 1024 * 1024) {
      showToast('图片过大或为空', 'warning')
      return
    }
    let binary = ''
    const chunk = 0x8000
    for (let i = 0; i < bytes.length; i += chunk) {
      binary += String.fromCharCode(...bytes.subarray(i, i + chunk))
    }
    const base64Data = btoa(binary)
    const songId = playerState.currentSong?.id || 'edit-cover'
    const uri = await cacheCoverBytes({
      cacheKey: `user-cover:${songId}:${Date.now()}`,
      base64Data,
    })
    if (!uri) {
      showToast('封面保存失败', 'danger')
      return
    }
    editCoverUri.value = uri
    editCoverCleared.value = false
    editCoverDirty.value = true
  } catch {
    showToast('读取图片失败', 'danger')
  }
}

const updateViewportSize = () => {
  viewportWidth.value = window.innerWidth
  viewportHeight.value = window.innerHeight
}

const clearLyricChromeIdleTimer = () => {
  if (lyricChromeIdleTimer !== null) {
    clearTimeout(lyricChromeIdleTimer)
    lyricChromeIdleTimer = null
  }
}

const scheduleLyricChromeHide = () => {
  clearLyricChromeIdleTimer()
  lyricChromeIdleTimer = setTimeout(() => {
    lyricChromeVisible.value = false
    lyricChromeIdleTimer = null
  }, LYRIC_FAB_IDLE_MS)
}

const revealLyricChrome = () => {
  if (activePanel.value !== 1) {
    return
  }
  lyricChromeVisible.value = true
  scheduleLyricChromeHide()
}

const hideLyricChromeImmediate = () => {
  lyricChromeVisible.value = false
  clearLyricChromeIdleTimer()
}

const toggleLyricTranslation = () => {
  showLyricTranslation.value = !showLyricTranslation.value
}

const onLyricTranslateClick = () => {
  revealLyricChrome()
  // 播放前歌词未加载（无翻译数据），移动端按钮仍常显：点击时提示
  if (!hasLyricTranslation.value) {
    showToast('当前歌曲暂无翻译歌词', 'default')
    return
  }
  toggleLyricTranslation()
}

const onLyricPlayClick = async () => {
  revealLyricChrome()
  await togglePlayback()
}

const onLyricPanelPointerUp = (event: PointerEvent) => {
  if (activePanel.value !== 1 || !playerState.currentSong) {
    return
  }
  // 已显示的 fab 自身 click 会重置 timer；此处避免重复与误触路径。
  if (event.target instanceof Element && event.target.closest('.lyric-fab')) {
    return
  }
  revealLyricChrome()
}

const onToggleRepeat = () => {
  setRepeatMode(queueState.repeatMode === 'one' ? 'all' : 'one')
}

const onToggleShuffle = () => {
  toggleShuffle()
}

const goToQueue = () => {
  openQueueOverlay()
}

const clearSeekUnlockTimer = () => {
  if (seekUnlockTimer !== null) {
    clearTimeout(seekUnlockTimer)
    seekUnlockTimer = null
  }
}

const lockSeekGesture = () => {
  seekGestureLocked.value = true
  clearSeekUnlockTimer()
}

const scheduleSeekUnlock = () => {
  clearSeekUnlockTimer()
  seekUnlockTimer = setTimeout(() => {
    seekGestureLocked.value = false
    seekUnlockTimer = null
  }, SEEK_CLICK_GUARD_MS)
}

const onProgressGestureStart = () => {
  lockSeekGesture()
  // 进度条手势与 overlay 全局手势隔离：清空已记录的触点，避免半成品横向/纵向手势。
  touchStartX.value = null
  touchStartY.value = null
  gestureDirection.value = null
  canDragDown.value = false
  isDraggingVertically.value = false
  clearDragOffsetImmediate()
}

const onProgressGestureEnd = () => {
  scheduleSeekUnlock()
}

const onPrevious = () => {
  if (seekGestureLocked.value) {
    return
  }
  void playPreviousFromQueue()
}

const onNext = () => {
  if (seekGestureLocked.value) {
    return
  }
  void playNextFromQueue()
}

const lyricArtist = computed(() => playerState.currentSong?.artist?.trim() || '')

/** 三行歌词上下文：上一行 / 当前行（高亮）/ 下一行 */
const toDisplayableUri = (uri: string): string => {
  if (!uri) {
    return ''
  }
  const normalizedUri = uri.trim().toLowerCase()
  if (normalizedUri.startsWith('data:') || normalizedUri.startsWith('blob:') || normalizedUri.includes(';base64,')) {
    return ''
  }

  return normalizedUri.startsWith('http://') || normalizedUri.startsWith('https://')
    ? uri
    : Capacitor.convertFileSrc(uri)
}

const currentLyrics = computed(() => playerState.lyrics || playerState.currentSong?.lyrics || '')
const currentCoverUri = computed(() => playerState.coverUri || playerState.currentSong?.coverUri || '')
const coverSrc = computed(() => toDisplayableUri(currentCoverUri.value))

/** 切歌无封面时沿用上一张可展示封面，避免背景/封面闪默认（#20） */
const stickyCoverSrc = ref('')

/** PlayerPage 保活后，打开/关闭必须清掉上次下滑位移，避免再打开半屏（#25） */
watch(playerOverlayVisible, (visible) => {
  if (visible) {
    // 重新打开时直接跳到最新播放位置，避免歌词从关闭前的旧行开始。
    hiddenLyricTime.value = playerState.position * 1000
  } else {
    hideLyricChromeImmediate()
  }
  resetDragState()
})

/** 松手回弹（原 CSS transition 220ms）：dragOffsetY 归零由 motion 命令式动画接管。
 * watch 仅承担兜底角色——任何路径把 dragOffsetY 归零（from>0）都会触发回弹，
 * 不再因「回弹动画进行中」而静默吞掉新的回弹请求（此前停止动画 commit 的中间值会残留半屏）。 */
watch(
  dragOffsetY,
  (next, prev) => {
    const from = prev ?? 0
    if (from <= 0 || next !== 0) {
      return
    }
    // 播放页已关闭：归零已由关闭路径的 resetDragState 兜底，不播回弹动画
    if (!playerOverlayVisible.value) {
      return
    }
    // 新触摸会话已开始（onTouchStart 主动清零残留）：回弹交给本次跟手，不播动画
    if (touchStartX.value !== null) {
      return
    }
    startRebound(from)
  },
  { flush: 'post' },
)

watch(activePanel, (panel) => {
  if (panel !== 1) {
    hideLyricChromeImmediate()
  }
})

watch(
  [() => playerState.currentSong?.id, coverSrc],
  ([songId, nextCover]) => {
    if (!songId) {
      stickyCoverSrc.value = ''
      return
    }
    if (nextCover) {
      stickyCoverSrc.value = nextCover
    }
  },
  { immediate: true },
)

const displayCoverSrc = computed(() => coverSrc.value || stickyCoverSrc.value)
const backgroundAlbumSrc = computed(() => displayCoverSrc.value)
const showAlbumBackground = computed(
  () => !!playerState.currentSong && !!backgroundAlbumSrc.value,
)

const lyricLines = computed<LyricLine[]>(() => {
  if (!currentLyrics.value) {
    return []
  }

  try {
    // 格式解析归 AMLL；业务只做 tlyric 挂载 + 双行 plain LRC 主译（mergeTranslation）。
    let lines: LyricLine[]
    if (playerState.lyricsFormat === 'ttml') {
      lines = parseTTML(currentLyrics.value).lines
    } else if (playerState.lyricsFormat === 'yrc') {
      lines = parseYrc(currentLyrics.value)
    } else if (playerState.lyricsFormat === 'qrc') {
      lines = parseQrc(currentLyrics.value)
    } else {
      lines = parseLrc(normalizeLrc(currentLyrics.value))
    }
    // attach tlyric + 合并同时间戳双主行（库已填 translatedLyric 则跳过合并）
    return prepareLyricLinesForDisplay(lines, playerState.lyricsTranslation)
  } catch {
    return []
  }
})

const displayLyricLines = computed<LyricLine[]>(() => {
  return applyLyricTranslationVisibility(lyricLines.value, showLyricTranslation.value)
})

/** AMLL 内部会缓存行节点，翻译显隐切换时强制重建保证立即生效（#26） */
const lyricPlayerKey = computed(() => `${playerState.currentSong?.id ?? 'none'}:${playerState.lyricsFormat ?? 'lrc'}:${showLyricTranslation.value ? 'translation-on' : 'translation-off'}`)

const hasLyrics = computed(() => lyricLines.value.length > 0)

/** 三行歌词上下文：上一行 / 当前行（高亮）/ 下一行 */
const lyricContext = computed(() => {
  if (!hasLyrics.value) {
    return { prev2: '', prev: '', current: '', next: '', next2: '' }
  }
  const targetMs = lyricRenderTime.value
  const lines = displayLyricLines.value
  const textOf = (line: LyricLine | undefined): string =>
    line ? (line.words ?? []).map((w) => w.word).join('') : ''
  let currentIdx = -1
  for (let i = 0; i < lines.length; i += 1) {
    const start = lines[i].startTime ?? 0
    if (start <= targetMs) {
      currentIdx = i
    } else {
      break
    }
  }
  const get = (i: number): string => (i >= 0 && i < lines.length ? textOf(lines[i]) : '')
  return {
    prev2: get(currentIdx - 2),
    prev: get(currentIdx - 1),
    current: get(currentIdx),
    next: get(currentIdx + 1),
    next2: get(currentIdx + 2),
  }
})

/** 五行滚动窗口（AMLL 式：当前行居中，切行时整体上移一行）
 * 始终固定五行（空行占位），避免行数变化导致页面跳动 */
const lyricWindow = computed(() => {
  const { prev2, prev, current, next, next2 } = lyricContext.value
  return [
    { key: 'prev2', text: prev2, isCurrent: false },
    { key: 'prev', text: prev, isCurrent: false },
    { key: 'current', text: current, isCurrent: true },
    { key: 'next', text: next, isCurrent: false },
    { key: 'next2', text: next2, isCurrent: false },
  ]
})

/** 歌词滚动动画（AMLL 式连续滚动：窗口整体上移一行，新行从底部自然进入）
 * displayedWindow 独立控制渲染，动画期间保持旧窗口，完成后换新窗口（视觉连续） */
const metaScrollEl = ref<HTMLElement | null>(null)
const displayedWindow = ref<{ key: string; text: string; isCurrent: boolean }[]>([])
let lyricScrollControls: AnimationPlaybackControls | null = null
let lyricScrolling = false

/* 非切行同步（初始/换歌/显隐）：直接更新显示窗口 */
watch(
  () => lyricWindow.value,
  (windowRows) => {
    if (!lyricScrolling) {
      displayedWindow.value = windowRows
    }
  },
  { immediate: true },
)

/* 切行：单段连续上移（丝滑缓动），完成后窗口上移一位 + 复位（视觉无跳） */
watch(
  () => lyricContext.value.current,
  (next, prev) => {
    if (!prev || next === prev || lyricScrolling) {
      return
    }
    /* 窄高屏单行模式（max-height: 520px）：仅当前行可见，滚动动画无意义，直接换窗口 */
    if (window.matchMedia('(max-height: 520px)').matches) {
      displayedWindow.value = lyricWindow.value
      return
    }
    /* 窗口整体重置（切歌/翻译显隐/seek 大跳）：新窗口 prev 行 ≠ 旧 current（prev），
     * 不是相邻切行——直接换窗口，避免误触滚动动画导致窗口下跳、第一行被裁切 */
    if (lyricWindow.value[1]?.text !== prev) {
      displayedWindow.value = lyricWindow.value
      return
    }
    const el = metaScrollEl.value
    if (!el) {
      return
    }
    lyricScrolling = true
    if (lyricScrollControls) {
      lyricScrollControls.stop()
    }
    // 窗口整体上移一行：next 行滚到当前行位，next2 从底部进入
    el.style.transform = 'translateY(0px)'
    lyricScrollControls = animate(
      el,
      { transform: 'translateY(-29.5px)' },
      {
        duration: 0.4,
        ease: [0.32, 0.72, 0, 1],
        onComplete: () => {
          // 窗口数据上移一位（与滚动后视觉一致）；清内联复位，回落 CSS --meta-window-offset（与动画终值一致，无跳变）
          displayedWindow.value = lyricWindow.value
          el.style.transform = ''
          lyricScrollControls = null
          lyricScrolling = false
        },
      },
    )
  },
  { flush: 'post' },
)

/** 有译文/音译才出翻译键；prepare 后的行或独立 tlyric 任一即可 */
const hasLyricTranslation = computed(() => {
  if (playerState.lyricsTranslation?.trim()) {
    return true
  }
  return lyricLines.value.some(
    (line) => !!line.translatedLyric?.trim() || !!line.romanLyric?.trim(),
  )
})

/** 宽屏无播放键且无译时不挂空 chrome */
const showLyricFloatingActions = computed(
  () => !!playerState.currentSong && (hasLyricTranslation.value || !isTabletLayout.value),
)

/** 本地来源化：无内嵌/sidecar 歌词即空态，引导去刮削页获取 */
const lyricEmptyTitle = computed(() => '暂无歌词')

const lyricEmptyDescription = computed(() => '未找到内嵌歌词或同目录同名 .lrc 文件，可在刮削页获取。')
const canSeek = computed(() => playerState.duration > 0)
const durationForSlider = computed(() => playerState.duration || 1)
const seekPreviewPosition = ref<number | null>(null)
const effectiveSeekPosition = computed(() => {
  // duration 未知（库内无时长且 native 未上报）：进度归 0，避免 position/max=1 显示 100%
  if (playerState.duration <= 0) {
    return 0
  }
  return seekPreviewPosition.value ?? playerState.position
})

const bufferHintVisible = ref(false)
let bufferHintTimer: ReturnType<typeof setTimeout> | null = null

const showBufferHint = () => {
  bufferHintVisible.value = true
  if (bufferHintTimer !== null) {
    clearTimeout(bufferHintTimer)
  }
  bufferHintTimer = setTimeout(() => {
    bufferHintVisible.value = false
    bufferHintTimer = null
  }, 1200)
}

const resetDragState = () => {
  // 打开/关闭播放页（保活重置）与卸载：终止回弹动画并把 DOM 残留位移一并清零，
  // 避免上一轮 stopRebound commit 的中间值在重开播放页时残留半屏（#25 / drag stuck）。
  clearDragOffsetImmediate()
  touchStartX.value = null
  touchStartY.value = null
  gestureDirection.value = null
  canDragDown.value = false
  isDraggingVertically.value = false
}

const goBack = () => {
  closePlayerOverlay()
}

const togglePlayback = async () => {
  if (isPlaying.value) {
    await pausePlayback()
    return
  }
  await resumePlayback()
}

const clampSeekTarget = (raw: number): number => {
  if (!Number.isFinite(raw) || raw < 0) {
    return 0
  }
  const buffered = playerState.bufferedPosition
  const duration = playerState.duration
  let max = duration > 0 ? duration : Number.POSITIVE_INFINITY
  if (buffered != null && Number.isFinite(buffered) && buffered >= 0) {
    max = duration > 0 ? Math.min(duration, buffered) : buffered
  }
  return Number.isFinite(max) ? Math.min(raw, max) : raw
}

/** 拖动中视觉 clamp 到已缓冲终点，并用本地 preview 驱动 h-range value。
 * 仅用户进度条手势写入 preview：程序化更新 value 时也可能触发 input，
 * 若误写 preview 会盖住 playerState.position，导致进度条冻结（#47）。
 */
const onSeekInput = (value: number) => {
  if (!seekGestureLocked.value) {
    return
  }
  const requested = value
  if (!Number.isFinite(requested)) {
    return
  }
  const clamped = clampSeekTarget(requested)
  seekPreviewPosition.value = clamped
  if (requested > clamped + 0.05) {
    showBufferHint()
  }
}

const onSeek = async (value: number) => {
  // ionChange 可能在 pointerup 之后触发；再锁一次并续期 debounce，覆盖 click 穿透窗口。
  lockSeekGesture()
  scheduleSeekUnlock()
  const requested = value
  if (!Number.isFinite(requested)) {
    seekPreviewPosition.value = null
    return
  }
  const clamped = clampSeekTarget(requested)
  if (requested > clamped + 0.05) {
    showBufferHint()
  }
  const ok = await seekPlayback(clamped)
  seekPreviewPosition.value = null
  if (!ok && playerState.bufferedPosition != null) {
    showBufferHint()
  }
}

/** 点击有时间戳的歌词行，seek 到该行起始秒；无效 startTime / 未缓冲区间不处理。 */
const onLyricLineClick = async (event: LyricLineMouseEvent) => {
  // 阻止冒泡到 overlay 手势，避免点击 seek 误切面板或关闭播放页。
  event.stopPropagation()
  event.preventDefault()
  lockSeekGesture()
  scheduleSeekUnlock()
  touchStartX.value = null
  touchStartY.value = null
  gestureDirection.value = null
  canDragDown.value = false
  isDraggingVertically.value = false
  clearDragOffsetImmediate()

  // LyricLineBase 通过 getLine() 暴露 LyricLine；startTime 单位为毫秒。
  const startMs = event.line?.getLine?.()?.startTime
  if (typeof startMs !== 'number' || !Number.isFinite(startMs) || startMs < 0) {
    return
  }
  const targetSec = startMs / 1000
  const ok = await seekPlayback(targetSec)
  if (!ok) {
    // 未缓冲区间：不 seek，轻提示（R3）
    showBufferHint()
  }
}

const onTouchStart = (event: TouchEvent) => {
  // 原生控件（进度条）或 seek 锁定期内，不启动 overlay 面板/下滑手势；
  // 一并终止在途回弹/残留位移，避免与控件交互打架。
  if (seekGestureLocked.value || isNativeInteractiveEvent(event)) {
    clearDragOffsetImmediate()
    touchStartX.value = null
    touchStartY.value = null
    gestureDirection.value = null
    isDraggingVertically.value = false
    canDragDown.value = false
    return
  }

  // 上一轮触摸序列可能被系统打断（touchcancel 丢失/多指/通知栏抢占）导致 dragOffsetY 残留：
  // 立即兜底清零（不播回弹动画），本次拖拽从 0 开始跟手。
  clearDragOffsetImmediate()
  const touch = event.changedTouches[0]
  touchStartX.value = touch?.clientX ?? null
  touchStartY.value = touch?.clientY ?? null
  gestureDirection.value = null
  isDraggingVertically.value = false
  canDragDown.value = canStartVerticalDismiss(event)
}

const onTouchMove = (event: TouchEvent) => {
  const startX = touchStartX.value
  const startY = touchStartY.value
  const touch = event.changedTouches[0]
  if (startX === null || startY === null || !touch) {
    return
  }

  // 进度条需要原生拖动；其余区域一律拦截默认滚动，防止穿透到底层歌曲列表。
  if (!isNativeInteractiveEvent(event)) {
    event.preventDefault()
  }

  const deltaX = touch.clientX - startX
  const deltaY = touch.clientY - startY

  if (!gestureDirection.value && Math.max(Math.abs(deltaX), Math.abs(deltaY)) > 8) {
    gestureDirection.value = Math.abs(deltaY) > Math.abs(deltaX) ? 'vertical' : 'horizontal'
  }

  // 歌词面板内滑动（含 AMLL 上下浏览）时短暂露出浮动按钮。
  if (
    activePanel.value === 1
    && isLyricPanelTarget(event)
    && Math.max(Math.abs(deltaX), Math.abs(deltaY)) > 8
  ) {
    revealLyricChrome()
  }

  if (gestureDirection.value !== 'vertical' || !canDragDown.value) {
    return
  }

  const nextOffset = Math.max(0, deltaY)
  if (reboundControls) {
    // 回弹动画未结束又起新拖拽：立即终止动画，恢复纯状态跟手
    stopRebound()
  }
  dragOffsetY.value = nextOffset
  isDraggingVertically.value = nextOffset > 0
}

/** 取出组件实例或原生元素的真实 DOM。 */
const unwrapRef = (value: HTMLElement | { $el?: HTMLElement } | null): HTMLElement | null => {
  if (!value) {
    return null
  }
  return value instanceof HTMLElement ? value : (value.$el ?? null)
}

/** 进度条范围元素（取代 `.progress-range` 选择器查询）。 */
const progressRangeEl = computed<HTMLElement | null>(() => unwrapRef(progressRangeRef.value))

/** 原生交互控件选择器——全是标准元素/属性选择器，非 class 标记，属合法声明式查询。 */
const INTERACTIVE_SELECTOR =
  'input, textarea, select, button, a, [role="button"], [contenteditable="true"]'

const isInteractiveElement = (el: Element): boolean => {
  // 进度条用 ref 识别（取代原 `.progress-range` class 选择器）
  const progressEl = progressRangeEl.value
  if (progressEl && progressEl.contains(el)) {
    return true
  }
  return Boolean(el.closest(INTERACTIVE_SELECTOR))
}

const isNativeInteractiveTarget = (target: EventTarget | null): boolean => {
  if (!(target instanceof Element)) {
    return false
  }
  if (isInteractiveElement(target)) {
    return true
  }
  return false
}

const isNativeInteractiveEvent = (event: TouchEvent | Event): boolean => {
  if (isNativeInteractiveTarget(event.target)) {
    return true
  }
  // Shadow DOM / 合成事件路径里目标可能是组件宿主（h-button 原生按钮上有 Shadow DOM）；
  // composedPath 能穿透 shadow，closest/contains 在 light DOM 上能识别。元素级 API，非标记类查询。
  if (!('composedPath' in event) || typeof event.composedPath !== 'function') {
    return false
  }
  return event.composedPath().some((node) => node instanceof Element && isInteractiveElement(node))
}

const onTouchEnd = (event: TouchEvent) => {
  const startX = touchStartX.value
  const endX = event.changedTouches[0]?.clientX
  const shouldDismiss = gestureDirection.value === 'vertical' && dragOffsetY.value >= getDismissThreshold()
  const skipPanelSwitch = seekGestureLocked.value || isNativeInteractiveEvent(event)

  touchStartX.value = null
  touchStartY.value = null
  gestureDirection.value = null
  canDragDown.value = false
  isDraggingVertically.value = false

  if (shouldDismiss) {
    // 超过阈值：直接收起关闭（原行为不变）
    clearDragOffsetImmediate()
    goBack()
    return
  }

  if (dragOffsetY.value > 0) {
    // 松手回弹：显式驱动（同步锁回拖拽终点 → 0.22s easeOut 回顶），watch 兜底再保一道。
    startRebound(dragOffsetY.value)
    dragOffsetY.value = 0
    return
  }

  // 进度条拖动期间/刚结束时，忽略横向位移，避免误切控制/歌词面板。
  if (skipPanelSwitch) {
    dragOffsetY.value = 0
    return
  }

  if (startX === null || endX === undefined || Math.abs(startX - endX) < 40) {
    // 歌词面板轻点：显示浮动 chrome（pointerup 也会兜底；touch 路径保证移动端一致）。
    if (
      activePanel.value === 1
      && isLyricPanelTarget(event)
      && !isNativeInteractiveEvent(event)
    ) {
      revealLyricChrome()
    }
    return
  }
  activePanel.value = endX < startX ? 1 : 0
}

const isLyricPanelTarget = (event: TouchEvent): boolean => {
  // 用 template ref 的 contains 判断触点是否落在歌词面板/歌词播放器内，取代 closest + classList.contains 标记类查询。
  const target = event.target
  const lyricPanelEl = lyricPanelRef.value
  if (lyricPanelEl instanceof HTMLElement && target instanceof Node && lyricPanelEl.contains(target)) {
    return true
  }
  const lyricPlayerEl = unwrapRef(lyricPlayerRef.value)
  return Boolean(lyricPlayerEl && target instanceof Node && lyricPlayerEl.contains(target))
}

const canStartVerticalDismiss = (event: TouchEvent): boolean => {
  // AMLL LyricPlayer 内部滚动基于 transform，非原生 scroll，无法被下方的原生滚动检测识别。
  // 触点位于歌词面板/歌词播放器内时，禁止 overlay 下滑关闭，避免歌词上下滚动误触发收起。
  if (isLyricPanelTarget(event)) {
    return false
  }
  return !event.composedPath().some((target) => {
    if (!(target instanceof HTMLElement)) {
      return false
    }
    return target.scrollHeight > target.clientHeight && target.scrollTop > 0
  })
}

const getDismissThreshold = (): number => {
  return Math.min(160, Math.max(96, window.innerHeight * 0.18))
}

const normalizeLrc = (lyrics: string): string => {
  return lyrics.replace(/\[((?:\d+:)*\d+),(\d+)\]/g, '[$1.$2]')
}

const formatTime = (value: number): string => {
  const totalSeconds = Math.max(0, Math.floor(value))
  const minutes = String(Math.floor(totalSeconds / 60)).padStart(2, '0')
  const seconds = String(totalSeconds % 60).padStart(2, '0')
  return `${minutes}:${seconds}`
}

/** 真机触摸序列被系统打断（通知栏下拉/多指/低端机事件丢失）时，touchend/touchcancel 可能不来：
 * 窗口失焦/页面隐藏兜底清零，避免 dragOffsetY 残留半屏。 */
const clearDragOnWindowHide = (): void => {
  if (dragOffsetY.value > 0 || reboundControls) {
    clearDragOffsetImmediate()
  }
}

onMounted(() => {
  updateViewportSize()
  window.addEventListener('resize', updateViewportSize)
  window.addEventListener('blur', clearDragOnWindowHide)
  document.addEventListener('visibilitychange', clearDragOnWindowHide)
  refreshCurrentSongScrapeState()
})

onUnmounted(() => {
  unsubscribeScrapeQueue()
  window.removeEventListener('resize', updateViewportSize)
  window.removeEventListener('blur', clearDragOnWindowHide)
  document.removeEventListener('visibilitychange', clearDragOnWindowHide)
  clearSeekUnlockTimer()
  clearLyricChromeIdleTimer()
  if (lyricScrollBackTimer) {
    clearTimeout(lyricScrollBackTimer)
    lyricScrollBackTimer = null
  }
  if (bufferHintTimer !== null) {
    clearTimeout(bufferHintTimer)
    bufferHintTimer = null
  }
  seekGestureLocked.value = false
  stopRebound()
  resetDragState()
})
</script>

<style scoped lang="scss">
/* ============ PlayerPage 沉浸式播放器（深色层） ============ */
.player-page {
  &__overlay {
    height: 100%;
    overflow: hidden;
    overscroll-behavior: none;
    touch-action: none;
    color: #fff;
  }

  /* MPopup 面板背景透明：下滑关闭时露出底下歌曲列表（而非 surface 白底） */
  &__popup {
    background: transparent !important;
  }

  &__drag-layer {
    position: relative;
    height: 100%;
    overflow: hidden;
    background: #05070d;
    will-change: transform;
    /* 椒盐式：固定头部 + 下方滑动区纵向排布；transform 拖拽不受 flex 影响 */
    display: flex;
    flex-direction: column;
  }

  &__bg {
    position: absolute;
    inset: 0;
    z-index: 0;
    overflow: hidden;
    opacity: 0.75;
  }

  &__bg-render {
    position: absolute;
    inset: 0;
    display: block;
    width: 100%;
    height: 100%;
  }

  &__fallback {
    position: absolute;
    inset: 0;
    z-index: 0;

    &--hidden {
      opacity: 0;
    }
  }

  &__empty {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    text-align: center;

    &-icon {
      font-size: 48px;
    }

    h1 {
      margin: 16px 0 8px;
      font-size: 20px;
      font-weight: 600;
    }

    p {
      margin: 0;
      font-size: 14px;
      line-height: 1.5;
      opacity: 0.75;
    }
  }

  /* 面板横向滑动容器（md+ 由全局 .panels 断点规则接管 transform:none） */
  &__panels {
    position: relative;
    z-index: 10;
    display: flex;
    width: 200%;
    height: 100%;
    max-height: 100%;
    overflow: hidden;

    /* 手机（<768px）：固定头部占顶部，滑动区收缩占剩余空间（椒盐式） */
    @media (max-width: 767.98px) {
      height: auto;
      flex: 1 1 auto;
      min-height: 0;
    }

    /* 平板（横屏 ≥768px 且宽>高，class 驱动）左右分栏：容器收缩回 100%、占剩余高度（底部控制条占位）。
       全局 index.scss 的 `width: auto` 与本规则同特异性，但 scoped 后注入恒覆盖全局，
       必须在此重申覆盖，否则容器保持 200% 宽、右侧歌词面板被裁出视口（08-15-tablet-player-layout） */
    .player-page--tablet & {
      width: 100%;
      height: auto;
      flex: 1 1 auto;
      min-height: 0;
    }
  }

  &__info-panel {
    position: relative; /* topbar 绝对定位锚点 */
  }

  &__info-inner {
    box-sizing: border-box;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: flex-end; /* 椒盐：内容块底部对齐 */
    gap: 14px;
    width: min(100%, 420px);
    height: 100%;
    margin: 0 auto;
    padding-top: 16px; /* 椒盐：顶部歌名/歌手区占位 */
    min-height: 0;
    overflow: hidden;

    /* 平板（横屏 ≥768px 且宽>高）：控制下移底部条后，封面垂直居中 */
    .player-page--tablet & {
      justify-content: center;
    }
  }

  /* 顶部歌名/歌手（椒盐：左上竖排小字，无返回/正在播放/更多） */
  &__song-head {
    flex: none;
    width: 100%;
    margin: 0;
    text-align: left;
    min-width: 0;

    /* 固定头部（手机 <768px）：位于滑动区上方，左右切面板不移动；
       顶部避让与 panel 一致（safe-area + 16px），左右 24px 与面板内容对齐。
       position/z-index 必须高于背景层（bg z-0 absolute），否则被封面背景蒙住变暗 */
    &--fixed {
      position: relative;
      z-index: 10;
      box-sizing: border-box;
      padding: calc(16px + var(--safe-area-inset-top, env(safe-area-inset-top, 0px))) 24px 0;

      /* 平板（横屏 ≥768px 且宽>高，class 驱动）：固定头部隐藏，由两面板各自的头部承担 */
      .player-page--tablet & {
        display: none;
      }
    }

    /* 面板内头部（仅平板显示；手机隐藏，避免与固定头部重复） */
    &--in-panel {
      display: none;

      .player-page--tablet & {
        display: block;
      }
    }
  }

  &__song-title {
    margin: 0;
    font-size: 20px;
    line-height: 1.3;
    font-weight: 600;
    letter-spacing: 0.01em;
    color: rgba(255, 255, 255, 0.95);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__song-artist {
    margin: 2px 0 0;
    font-size: 13px;
    line-height: 1.4;
    color: rgba(255, 255, 255, 0.6);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  /* 大封面（椒盐：铺满上部约 49% 屏高，歌名/歌手浮层） */
  &__cover-hero {
    position: relative;
    display: flex;
    align-items: center;
    justify-content: center;
    flex: 1 1 auto;
    min-height: 0;
    max-height: min(50vh, 420px);
    width: 100%;
    overflow: hidden;
  }

  &__cover-hero-img {
    /* 正方形封面：宽高均 auto + aspect-ratio 1:1，max 约束按比例 contain（边长 = min(容器宽, 容器高)），空间越大封面越大且恒为正方形 */
    width: auto;
    height: auto;
    max-width: 100%;
    max-height: 100%;
    aspect-ratio: 1;
    object-fit: cover;
    border-radius: var(--m-radius-card);
  }

  &__cover-hero-placeholder {
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 56px;
  }

  /* 歌曲信息三行（椒盐：作曲/编曲/歌词，左对齐小字） */
  /* 五行歌词窗口（AMLL 式连续滚动） */
  &__song-meta {
    flex: none;
    width: 100%;
    margin: 0 0 18px; /* 与进度条拉开间距 */
    text-align: left;
    min-width: 0;
    height: 79px; /* 三行视口高度，固定不跳动 */
    overflow: hidden;
    position: relative;

    /* 平板（横屏 ≥768px 且宽>高，class 驱动）左侧不展示三行歌词：左右分栏时歌词由右侧歌词页承担（08-15-tablet-player-layout） */
    .player-page--tablet & {
      display: none;
    }
  }

  &__meta-window {
    /* 稳态偏移 token：三行模式 -29.5px（= 一行 19.5px + 行距 10px，当前行居中视口、三行完整可见）；窄高屏单行模式由 media query 覆盖 */
    --meta-window-offset: -29.5px;

    display: flex;
    flex-direction: column;
    width: 100%;
    /* 初始定位：当前行（第 3 位）居中于视口；切行由 animate 驱动上移；JS 动画结束清内联后回落此值 */
    transform: translateY(var(--meta-window-offset));
    will-change: transform;
  }

  /* 三行歌词上下文（AMLL 风格：当前行高亮放大、前后行淡化缩小） */
  &__meta-line {
    margin: 0;
    font-size: 13px;
    line-height: 1.5;
    color: rgba(255, 255, 255, 0.6);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    transform-origin: left center; /* 缩放从左缘开始，三行保持左对齐不偏移 */
  }

  &__meta-line + &__meta-line {
    margin-top: 10px; /* 椒盐三行行距 ~24dp，此处 10px 视觉接近 */
  }

  &__meta-current {
    color: rgba(255, 255, 255, 0.92);
  }

  /* 上下太窄（横屏/车机极限）：三行歌词自动收成一行（只显示当前行） */
  @media (max-height: 520px) {
    &__song-meta {
      height: 19.5px; /* 单行视口（覆盖三行 79px） */

      .player-page__meta-window {
        /* 仅当前行可见：抵消其 margin-top 10px，让当前行对齐视口（否则被推出视口） */
        --meta-window-offset: -10px;
      }

      .player-page__meta-line:not(.player-page__meta-current) {
        display: none;
      }
    }

    &__cover-hero-img {
      width: min(34vw, 150px);
    }
  }

  &__progress-area {
    flex: none;
    width: 100%;
  }

  &__progress-range {
    width: 100%;
    color: var(--m-primary);
    accent-color: var(--m-primary);
    cursor: pointer;
    touch-action: manipulation;

    /* 隐藏 thumbWrap（原 [&_[style*='inset-inline-start']]:hidden） */
    :deep([style*='inset-inline-start']) {
      display: none;
    }
  }

  &__time-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-top: 2px;
    font-size: 12px;
    font-variant-numeric: tabular-nums;
    color: rgba(255, 255, 255, 0.68);
  }

  &__buffer-hint {
    font-size: 11px;
    color: rgba(255, 255, 255, 0.55);
  }

  &__controls {
    flex: none;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: clamp(24px, 10vw, 44px);
    width: 100%;
    margin: 0;
    touch-action: manipulation;
  }

  &__mode-bar {
    flex: none;
    display: flex;
    justify-content: space-between;
    align-items: center;
    width: 100%;
    max-width: 320px;
    margin: 0;
    touch-action: manipulation;
  }

  /* 平板底部全宽控制条（仅横屏平板 ≥768px 且宽>高渲染；手机/竖屏由 info-panel 内控件承担） */
  &__bottom-bar {
    flex: none;
    position: relative;
    z-index: 10;
    box-sizing: border-box;
    padding: 6px 24px
      calc(8px + var(--safe-area-inset-bottom, env(safe-area-inset-bottom, 0px)));
    /* 半透明渐变叠加 AMLL 背景，保证控件可读且保持沉浸 */
    background: linear-gradient(
      180deg,
      rgba(5, 7, 13, 0) 0%,
      rgba(5, 7, 13, 0.55) 100%
    );
    touch-action: manipulation;
  }

  &__bottom-progress {
    width: 100%;
  }

  &__bottom-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
    margin-top: 2px;
  }

  /* 底部条按钮组：覆盖 controls/mode-bar 的全宽布局为自适应三段式 */
  &__bottom-mode {
    flex: none;
    display: flex;
    align-items: center;
    gap: 4px;
    width: auto;
    max-width: none;
    margin: 0;
  }

  &__bottom-controls {
    flex: none;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: clamp(20px, 5vw, 44px);
    width: auto;
    margin: 0;
  }

  /* 播放器控制按钮（深底白字系；MIconButton 透明底 + currentColor，按下图标变暗） */
  &__side-btn {
    color: rgba(255, 255, 255, 0.9);
  }

  /* 播放按钮：无圆底，与侧边按钮同尺寸（用户要求统一大小） */
  &__play-btn {
    color: rgba(255, 255, 255, 0.92);
  }

  &__mode-btn {
    color: rgba(255, 255, 255, 0.8);
  }

  &__icon-lg {
    width: 28px;
    height: 28px;
    /* lucide 双轨：闭合图形（skip 三角）fill 实心 + 线条（skip 竖条）stroke 描边 */
    fill: currentColor;
    stroke: currentColor;
    stroke-width: 2;
    stroke-linecap: round;
    stroke-linejoin: round;
  }

  &__icon-xl {
    width: 40px;
    height: 40px;
    fill: currentColor;
    stroke: currentColor;
    stroke-width: 2;
    stroke-linecap: round;
    stroke-linejoin: round;
  }

  &__icon {
    width: 20px;
    height: 20px;
    /* lucide 默认渲染：svg 自带 fill=none + stroke=currentColor，无需 CSS 覆盖 */
  }

  /* 歌词页 */
  &__lyric-panel {
    display: flex;
    flex-direction: column;
    align-items: stretch;
    justify-content: flex-start;
    overflow: hidden;
  }

  &__lyric-header {
    flex: none;
    width: 100%;
    text-align: left;
    min-width: 0;

    /* 手机（<768px）：头部由固定头部承担，面板内不再重复显示；
       平板（横屏 ≥768px 且宽>高）：仍由全局 `.player-overlay--tablet .lyric-header { display: none }` 隐藏
       （左栏 in-panel 头部承担，避免左右重复；见 index.scss） */
    display: none;
  }

  /* 手机控件区（info-panel 内）：平板下由底部控制条承担，隐藏 */
  &__info-controls {
    display: contents; /* 手机保持现有 flex 布局子项展开，不改变 info-inner 结构 */

    .player-page--tablet & {
      display: none;
    }
  }

  &__lyric-title {
    margin: 0;
    font-size: 20px;
    line-height: 1.3;
    font-weight: 600;
    letter-spacing: 0.01em;
    color: rgba(255, 255, 255, 0.95);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__lyric-artist {
    margin: 2px 0 0;
    font-size: 13px;
    line-height: 1.4;
    color: rgba(255, 255, 255, 0.6);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__lyric-player {
    display: block;
    position: relative;
    flex: 1 1 auto;
    width: 100%;
    min-height: 0;
    height: auto;
  }

  &__lyric-empty {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    text-align: center;
    flex: 1 1 auto;
    width: 100%;
    min-height: 0;
    overflow: hidden;

    h2 {
      margin: 0 0 8px;
      font-size: 17px;
      font-weight: 600;
    }

    p {
      margin: 0;
      font-size: 13px;
      line-height: 1.5;
      opacity: 0.65;
    }
  }

  &__lyric-fabs {
    position: absolute;
    left: 12px;
    right: 12px;
    bottom: calc(8px + var(--safe-area-inset-bottom, env(safe-area-inset-bottom, 0px)));
    z-index: 3;
    display: flex;
    align-items: center;
    pointer-events: none;

    &--split {
      justify-content: space-between;
    }

    &--end {
      justify-content: flex-end;
    }
  }

  &__lyric-fab {
    color: rgba(255, 255, 255, 0.8);
  }

  &__lyric-play-fab {
    color: #fff;
  }
}

/* ============ 编辑歌曲信息（sheet 内，主题色系） ============ */
.player-page {
  &__sheet-title {
    padding: 16px 0 8px;
    text-align: center;
    font-size: 17px;
    font-weight: 600;
    color: var(--m-text);
  }

  &__edit-form {
    display: flex;
    flex-direction: column;
    gap: 12px;
    padding: 0 16px 16px;
  }

  &__seg {
    display: flex;
    border-radius: var(--m-radius-sm);
    background-color: var(--m-surface-2);
    padding: 3px;
  }

  &__seg-btn {
    flex: 1;
    height: 34px;
    border: none;
    border-radius: var(--m-radius-sm);
    background: transparent;
    font-size: 15px;
    font-weight: 500;
    font-family: inherit;
    color: var(--m-text-secondary);
    transition: background-color 0.2s, color 0.2s;
    -webkit-tap-highlight-color: rgba(0, 0, 0, 0);

    &--active {
      background-color: var(--m-primary);
      color: #fff;
      box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
    }
  }

  &__cloud-card {
    display: flex;
    flex-direction: column;
    gap: var(--m-spacing-sub);
    border: 1px solid var(--m-hairline);
    border-radius: var(--m-radius-card);
    padding: 12px;
  }

  &__cloud-field {
    display: flex;
    flex-direction: column;
    gap: 8px;
  }

  &__label {
    margin: 0;
    font-size: 15px;
    font-weight: 500;
    color: var(--m-text);
  }

  &__muted {
    margin: 0;
    font-size: 15px;
    color: var(--m-text-secondary);
  }

  &__row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
  }

  &__pill-row {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
  }

  &__pill {
    height: 30px;
    padding: 0 var(--m-spacing-sub);
    border: 1px solid var(--m-hairline);
    border-radius: var(--m-radius-md);
    background: transparent;
    font-size: 15px;
    font-family: inherit;
    color: var(--m-text-secondary);
    transition: background-color 0.2s, color 0.2s, border-color 0.2s;
    -webkit-tap-highlight-color: rgba(0, 0, 0, 0);

    &--active {
      background-color: var(--m-primary);
      border-color: transparent;
      color: #fff;
    }

    &:disabled {
      opacity: 0.6;
      pointer-events: none;
    }
  }

  &__self-start {
    align-self: flex-start;
  }

  &__cloud-result {
    display: flex;
    flex-direction: column;
    gap: 2px;
  }

  &__cand-list {
    display: flex;
    flex-direction: column;
    gap: 4px;
    max-height: 144px;
    overflow-y: auto;
  }

  &__cand-btn {
    padding: 6px;
    border: none;
    border-radius: 8px;
    background: transparent;
    text-align: left;
    font-size: 15px;
    font-family: inherit;
    color: var(--m-text);

    &--selected {
      background-color: rgba(0, 0, 0, 0.05);
    }
  }

  &__cand-source {
    opacity: 0.6;
  }

  &__cover-thumb {
    width: 56px;
    height: 56px;
    border-radius: var(--m-radius-sm);
    object-fit: cover;
  }

  &__cover-cands {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    max-height: 160px;
    overflow-y: auto;
  }

  &__cover-cand {
    padding: 0;
    border: none;
    border-radius: var(--m-radius-sm);
    overflow: hidden;
    background: transparent;

    &--selected {
      box-shadow: 0 0 0 2px var(--m-primary);
    }
  }

  &__cover-cand-img {
    display: block;
    width: 48px;
    height: 48px;
    object-fit: cover;
  }

  &__cloud-apply {
    display: flex;
    flex-direction: column;
    gap: 6px;
    padding-top: 2px;
  }

  &__check-row {
    display: flex;
    flex-wrap: wrap;
    gap: 6px 12px;
  }

  &__check-label {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    font-size: 15px;
    color: var(--m-text);
  }

  &__field-input {
    display: block;
    box-sizing: border-box;
    width: 100%;
    height: 40px;
    border: none;
    outline: none;
    background: transparent;
    font-size: 16px;
    color: var(--m-text);

    &::placeholder {
      color: var(--m-text-tertiary);
    }
  }

  &__cover-edit {
    display: flex;
    flex-direction: column;
    gap: 2px;
  }

  &__cover-edit-row {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  &__cover-edit-preview {
    flex: none;
    width: 64px;
    height: 64px;
    border-radius: 10px;
    object-fit: cover;
  }

  &__cover-edit-placeholder {
    flex: none;
    width: 64px;
    height: 64px;
    border-radius: 10px;
    display: flex;
    align-items: center;
    justify-content: center;
    background-color: rgba(0, 0, 0, 0.05);
    font-size: 24px;
    color: var(--m-text);
  }

  &__cover-edit-actions {
    display: flex;
    flex-direction: column;
    gap: 2px;
    min-width: 0;
  }

  &__hidden-input {
    display: none;
  }

  &__lyrics-input {
    display: block;
    box-sizing: border-box;
    width: 100%;
    height: 40px;
    padding: 8px 0;
    border: none;
    outline: none;
    background: transparent;
    font-size: 16px;
    font-family: inherit;
    color: var(--m-text);
    resize: none;

    &::placeholder {
      color: var(--m-text-tertiary);
    }
  }

  &__lyrics-preview {
    margin: 0;
    max-height: 112px;
    overflow-y: auto;
    white-space: pre-wrap;
    font-size: 15px;
    color: var(--m-text);
    opacity: 0.8;
  }

  &__error-text {
    margin: 0;
    font-size: 15px;
    color: #ff3b30;
  }

  &__form-actions {
    display: flex;
    justify-content: flex-end;
    gap: var(--m-spacing-sub);
    padding-top: 8px;
  }
}

/* ============ 暗色主题变体（编辑弹窗内） ============ */
:global(.dark .player-page__seg) {
  background-color: rgba(255, 255, 255, 0.1);
}

:global(.dark .player-page__seg-btn) {
  color: var(--m-text-secondary);
}

:global(.dark .player-page__seg-btn--active) {
  background-color: rgba(255, 255, 255, 0.15);
  color: var(--m-text);
}

:global(.dark .player-page__cloud-card) {
  border-color: rgba(255, 255, 255, 0.15);
}

:global(.dark .player-page__pill) {
  border-color: rgba(255, 255, 255, 0.15);
  color: var(--m-text-secondary);
}

:global(.dark .player-page__pill--active) {
  background-color: var(--m-primary);
  border-color: transparent;
  color: #fff;
}

:global(.dark .player-page__cand-btn--selected) {
  background-color: rgba(255, 255, 255, 0.1);
}

:global(.dark .player-page__cover-edit-placeholder) {
  background-color: rgba(255, 255, 255, 0.1);
}
</style>

