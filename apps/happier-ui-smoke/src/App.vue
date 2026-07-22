<template>
  <main class="smoke">
    <header class="smoke__header">
      <h1 class="smoke__title">happier-ui 冒烟</h1>
      <p class="smoke__lead">
        第二宿主：纯 Vue + Capacitor 配置，无 Muses 业务、无 Ionic 壳。
      </p>
      <div class="smoke__swatch" aria-hidden="true" />
    </header>

    <section class="smoke__section" aria-labelledby="empty-heading">
      <h2 id="empty-heading" class="smoke__section-title">HEmptyState</h2>
      <h-empty-state
        title="暂无内容"
        description="这是跨项目接入 happier-ui 的空态示例。"
      />
    </section>

    <section class="smoke__section" aria-labelledby="list-heading">
      <h2 id="list-heading" class="smoke__section-title">HListRow</h2>
      <div class="smoke__list">
        <h-list-row
          title="正在播放示例"
          subtitle="副标题 · playing"
          playing
          show-playing-indicator
        >
          <template #start>
            <span class="smoke__thumb" aria-hidden="true" />
          </template>
        </h-list-row>
        <h-list-row title="普通行" subtitle="仅 start 槽色块">
          <template #start>
            <span class="smoke__thumb smoke__thumb--muted" aria-hidden="true" />
          </template>
        </h-list-row>
      </div>
    </section>

    <section class="smoke__section" aria-labelledby="setting-heading">
      <h2 id="setting-heading" class="smoke__section-title">HSettingRow</h2>
      <h-setting-row
        label="示例开关"
        description="end 槽使用原生 checkbox（无 ion-toggle）"
      >
        <template #end>
          <input v-model="enabled" type="checkbox" aria-label="示例开关" />
        </template>
      </h-setting-row>
    </section>

    <section class="smoke__section" aria-labelledby="icon-heading">
      <h2 id="icon-heading" class="smoke__section-title">HIconButton</h2>
      <p class="smoke__hint">使用默认 slot 内联 SVG，不依赖 @ionic/vue。</p>
      <h-icon-button ariaLabel="示例操作" @click="onPing">
        <svg
          width="22"
          height="22"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          aria-hidden="true"
        >
          <path d="M5 12h14M12 5l7 7-7 7" />
        </svg>
      </h-icon-button>
      <p v-if="pingCount > 0" class="smoke__ping">点击次数：{{ pingCount }}</p>
    </section>
  </main>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import {
  HEmptyState,
  HIconButton,
  HListRow,
  HSettingRow,
} from 'happier-ui'

const enabled = ref(true)
const pingCount = ref(0)

const onPing = () => {
  pingCount.value += 1
}
</script>

<style scoped>
.smoke {
  max-width: 28rem;
  margin: 0 auto;
  padding: var(--h-space-lg, 16px);
  padding-bottom: calc(var(--h-space-xl, 24px) + env(safe-area-inset-bottom, 0px));
}

.smoke__header {
  margin-bottom: var(--h-space-xl, 24px);
}

.smoke__title {
  margin: 0 0 var(--h-space-sm, 8px);
  font-size: 1.25rem;
  color: var(--h-color-primary, #006fee);
}

.smoke__lead {
  margin: 0 0 var(--h-space-md, 12px);
  font-size: var(--h-font-body-sm, 13px);
  color: var(--h-color-ink-muted, #666);
  text-wrap: pretty;
}

.smoke__swatch {
  width: 100%;
  height: 8px;
  border-radius: var(--h-radius-pill, 999px);
  background: var(--h-color-primary, #006fee);
}

.smoke__section {
  margin-bottom: var(--h-space-xl, 24px);
}

.smoke__section-title {
  margin: 0 0 var(--h-space-sm, 8px);
  font-size: var(--h-font-label, 12px);
  font-weight: 600;
  letter-spacing: 0.04em;
  text-transform: uppercase;
  color: var(--h-color-ink-muted, #666);
}

.smoke__list {
  border: 1px solid var(--h-color-separator, #e0e0e0);
  border-radius: var(--h-radius-md, 12px);
  overflow: hidden;
}

.smoke__thumb {
  display: block;
  width: var(--h-cover-size-sm, 48px);
  height: var(--h-cover-size-sm, 48px);
  border-radius: var(--h-radius-cover-sm, 8px);
  background: var(--h-color-primary, #006fee);
  opacity: 0.35;
}

.smoke__thumb--muted {
  background: var(--h-color-ink-muted, #92949c);
  opacity: 0.25;
}

.smoke__hint {
  margin: 0 0 var(--h-space-sm, 8px);
  font-size: var(--h-font-body-sm, 13px);
  color: var(--h-color-ink-muted, #666);
}

.smoke__ping {
  margin: var(--h-space-sm, 8px) 0 0;
  font-size: var(--h-font-body-sm, 13px);
  color: var(--h-color-primary, #006fee);
}
</style>
