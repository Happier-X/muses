<template>
  <div
    class="tabs-layout"
    @touchstart="onPageTouchStart"
    @touchmove="onPageTouchMove"
    @touchend="onPageTouchEnd"
    @touchcancel="cancelTouchGesture"
  >
    <div class="tabs-layout__body">
      <aside
        v-if="isTablet && isTabsRoute"
        class="tabs-layout__aside"
        aria-label="主导航"
      >
        <div class="tabs-layout__panel">
          <nav aria-label="主导航" class="tabs-layout__nav tabs-layout__nav--primary">
            <RouterLink
              v-for="item in primaryNavItems"
              :key="item.to"
              :to="item.to"
              class="tabs-layout__nav-link"
              :class="{ 'tabs-layout__nav-link--active': isNavActive(item) }"
              :aria-current="isNavActive(item) ? 'page' : undefined"
            >
              <span class="tabs-layout__nav-icon-shell" aria-hidden="true">
                <component :is="item.icon" class="tabs-layout__nav-icon" />
              </span>
              <span class="tabs-layout__nav-label">{{ item.label }}</span>
            </RouterLink>
          </nav>
          <nav aria-label="辅助导航" class="tabs-layout__nav tabs-layout__nav--secondary">
            <RouterLink
              v-for="item in secondaryNavItems"
              :key="item.to"
              :to="item.to"
              class="tabs-layout__nav-link"
              :class="{ 'tabs-layout__nav-link--active': isNavActive(item) }"
              :aria-current="isNavActive(item) ? 'page' : undefined"
            >
              <span class="tabs-layout__nav-icon-shell" aria-hidden="true">
                <component :is="item.icon" class="tabs-layout__nav-icon" />
              </span>
              <span class="tabs-layout__nav-label">{{ item.label }}</span>
            </RouterLink>
          </nav>
        </div>
      </aside>

      <motion.div
        ref="navigationTrackRef"
        class="tabs-layout__track"
        :animate="{ x: isTablet ? 0 : navigationTranslateX }"
        :transition="drawerTransition"
      >
        <aside
          v-if="!isTablet && isTabsRoute"
          ref="drawerPanelRef"
          class="tabs-layout__drawer"
          :inert="!drawerRendered || undefined"
          :aria-hidden="!drawerRendered ? 'true' : undefined"
          aria-label="主导航"
        >
          <div class="tabs-layout__panel">
            <nav aria-label="主导航" class="tabs-layout__nav tabs-layout__nav--primary">
              <RouterLink
                v-for="item in primaryNavItems"
                :key="item.to"
                ref="drawerLinkRefs"
                :to="item.to"
                class="tabs-layout__nav-link tabs-layout__drawer-link"
                :class="{ 'tabs-layout__nav-link--active': isNavActive(item) }"
                :aria-current="isNavActive(item) ? 'page' : undefined"
                @click="onDrawerNavigation"
              >
                <span class="tabs-layout__nav-icon-shell" aria-hidden="true">
                  <component :is="item.icon" class="tabs-layout__nav-icon" />
                </span>
                <span class="tabs-layout__nav-label">{{ item.label }}</span>
              </RouterLink>
            </nav>
            <nav aria-label="辅助导航" class="tabs-layout__nav tabs-layout__nav--secondary">
              <RouterLink
                v-for="item in secondaryNavItems"
                :key="item.to"
                ref="drawerLinkRefs"
                :to="item.to"
                class="tabs-layout__nav-link tabs-layout__drawer-link"
                :class="{ 'tabs-layout__nav-link--active': isNavActive(item) }"
                :aria-current="isNavActive(item) ? 'page' : undefined"
                @click="onDrawerNavigation"
              >
                <span class="tabs-layout__nav-icon-shell" aria-hidden="true">
                  <component :is="item.icon" class="tabs-layout__nav-icon" />
                </span>
                <span class="tabs-layout__nav-label">{{ item.label }}</span>
              </RouterLink>
            </nav>
          </div>
        </aside>
        <main
          class="tabs-layout__main"
          :class="{ 'tabs-layout__main--tabbed': isTabsRoute }"
          :inert="drawerInteractive || undefined"
          :aria-hidden="drawerInteractive ? 'true' : undefined"
        >
          <RouterView />
        </main>
      </motion.div>
    </div>

    <!-- 透明关闭交互区（仅移动端抽屉打开态）：主页面被推开后 inert，汉堡按钮
         位于其内无法再点击；此层覆盖被推开的可视主页面区域（侧栏 0..50vw 之外），
         承接关闭点击，点击即关闭抽屉。无背景色不影响推屏视觉，层级低于
         MiniPlayer(1000) 与全局弹层，不拦截 touch 事件以保留左滑关闭手势。 -->
    <div
      v-if="!isTablet && isTabsRoute && drawerOpen"
      class="tabs-layout__drawer-dismiss"
      aria-hidden="true"
      @click="closeDrawer"
    />
  </div>
</template>

<script setup lang="ts">
import { animate, motion } from 'motion-v'
import { computed, nextTick, onMounted, onUnmounted, provide, readonly, ref, watch } from 'vue'
import { RouterLink, RouterView, useRoute } from 'vue-router'
import type { ComponentPublicInstance } from 'vue'
import { albums, list, musicalNotes, person, radio, settings } from '@/icons'
import { navigationDrawerKey } from '@/features/navigation/drawer'
import { playerOverlayVisible, queueOverlayVisible } from '@/features/player/overlay'

interface NavigationItem {
  to: string
  label: string
  icon: object
  childPrefixes?: string[]
}

const navItems: NavigationItem[] = [
  { to: '/tabs/songs', label: '歌曲', icon: musicalNotes },
  { to: '/tabs/albums', label: '专辑', icon: albums, childPrefixes: ['/tabs/library/album/'] },
  { to: '/tabs/artists', label: '艺术家', icon: person, childPrefixes: ['/tabs/library/artist/'] },
  { to: '/tabs/playlists', label: '歌单', icon: list, childPrefixes: ['/tabs/playlists/'] },
  { to: '/tabs/sources', label: '音源', icon: radio },
  { to: '/tabs/settings', label: '设置', icon: settings },
]

/** 主菜单（曲库）：歌曲/专辑/艺术家/歌单（椒盐侧边栏主区 5 项的口径，Muses 取自有页面） */
const primaryNavItems = navItems.slice(0, 4)
/** 次菜单（工具）：音源/设置（对齐椒盐主区与次区分组的视觉） */
const secondaryNavItems = navItems.slice(4)

const HORIZONTAL_LOCK_PX = 8
const SETTLE_RATIO = 0.25
const FAST_SWIPE_PX_PER_MS = 0.5
const route = useRoute()
const viewportWidth = ref(typeof window === 'undefined' ? 0 : window.innerWidth)
const drawerOpen = ref(false)
const drawerRendered = ref(false)
const drawerDragging = ref(false)
const navigationTranslateX = ref(-viewportWidth.value * 0.5)
const navigationTrackRef = ref<ComponentPublicInstance | HTMLElement | null>(null)
const drawerPanelRef = ref<ComponentPublicInstance | HTMLElement | null>(null)
const drawerLinkRefs = ref<ComponentPublicInstance[]>([])
const drawerTrigger = ref<HTMLElement | null>(null)
const prefersReducedMotion = ref(false)
const isTablet = computed(() => viewportWidth.value >= 768)
const isTabsRoute = computed(() => route.path === '/tabs' || route.path.startsWith('/tabs/'))
const drawerInteractive = computed(() => drawerOpen.value || drawerDragging.value)
const drawerWidth = computed(() => viewportWidth.value * 0.5)
const drawerTransition = computed(() => ({
  duration: prefersReducedMotion.value ? 0 : drawerDragging.value ? 0 : 0.24,
  ease: [0.32, 0.72, 0, 1],
}))

let activeTouchId: number | null = null
let touchStartX = 0
let touchStartY = 0
let touchStartTime = 0
let gestureMode: 'opening' | 'closing' | null = null
let drawerGeneration = 0
let drawerAnimation: ReturnType<typeof animate> | null = null
let mediaQuery: MediaQueryList | null = null

provide(navigationDrawerKey, {
  expanded: readonly(drawerOpen),
  open: openDrawer,
})

const isNavActive = (item: NavigationItem) => {
  if (route.path === item.to || route.path.startsWith(`${item.to}/`)) return true
  return item.childPrefixes?.some((prefix) => route.path.startsWith(prefix)) ?? false
}

function getElement(target: ComponentPublicInstance | HTMLElement | null) {
  if (!target) return null
  return target instanceof HTMLElement ? target : target.$el as HTMLElement
}

function getPanelElement() {
  return getElement(drawerPanelRef.value)
}

function getTrackElement() {
  return getElement(navigationTrackRef.value)
}

function stopDrawerAnimation() {
  drawerAnimation?.stop()
  drawerAnimation = null
}

function openDrawer(trigger?: HTMLElement | null) {
  if (isTablet.value || !isTabsRoute.value || playerOverlayVisible.value || queueOverlayVisible.value) return
  const openGeneration = ++drawerGeneration
  stopDrawerAnimation()
  drawerTrigger.value = trigger ?? drawerTrigger.value
  drawerRendered.value = true
  drawerOpen.value = true
  drawerDragging.value = false
  navigationTranslateX.value = -drawerWidth.value
  void nextTick(() => {
    if (openGeneration !== drawerGeneration) return
    navigationTranslateX.value = 0
    drawerLinkRefs.value[0]?.$el?.focus()
  })
}

async function closeDrawer() {
  if (!drawerRendered.value) return
  const closeGeneration = ++drawerGeneration
  resetTouchGesture()
  drawerOpen.value = false
  const targetX = -drawerWidth.value
  const track = getTrackElement()
  stopDrawerAnimation()

  if (track && !prefersReducedMotion.value && !isTablet.value) {
    const animation = animate(
      track,
      { x: targetX },
      { duration: 0.24, ease: [0.32, 0.72, 0, 1] },
    )
    drawerAnimation = animation
    await animation
    if (drawerAnimation === animation) drawerAnimation = null
  }

  if (closeGeneration !== drawerGeneration) return
  navigationTranslateX.value = targetX
  drawerRendered.value = false
  drawerTrigger.value?.focus()
}

function onDrawerNavigation() {
  closeDrawer()
}

function isGestureBlocked(target: EventTarget | null) {
  if (!(target instanceof Element)) return false
  return !!target.closest('.m-popup, .m-sheet, .m-dialog, .m-actions, .m-toast, input, textarea, select, [contenteditable="true"]')
}

function findTrackedTouch(event: TouchEvent, changed = false) {
  const touches = changed ? event.changedTouches : event.touches
  return Array.from(touches).find((touch) => touch.identifier === activeTouchId) ?? null
}

function beginTouchGesture(event: TouchEvent, mode: 'opening' | 'closing') {
  if (activeTouchId !== null || isGestureBlocked(event.target)) return
  const touch = event.changedTouches[0]
  if (!touch) return
  activeTouchId = touch.identifier
  touchStartX = touch.clientX
  touchStartY = touch.clientY
  touchStartTime = performance.now()
  gestureMode = mode
}

function onPageTouchStart(event: TouchEvent) {
  if (isTablet.value || !isTabsRoute.value || playerOverlayVisible.value || queueOverlayVisible.value) return
  if (drawerOpen.value) {
    beginTouchGesture(event, 'closing')
  } else if (!drawerRendered.value) {
    beginTouchGesture(event, 'opening')
  }
}

function updateTouchGesture(event: TouchEvent) {
  if (activeTouchId === null || !gestureMode) return
  const touch = findTrackedTouch(event)
  if (!touch) return
  const dx = touch.clientX - touchStartX
  const dy = touch.clientY - touchStartY

  if (!drawerDragging.value) {
    if (Math.abs(dx) < HORIZONTAL_LOCK_PX) return
    if (Math.abs(dx) <= Math.abs(dy)) {
      resetTouchGesture()
      return
    }
    if ((gestureMode === 'opening' && dx <= 0) || (gestureMode === 'closing' && dx >= 0)) {
      resetTouchGesture()
      return
    }

    stopDrawerAnimation()
    drawerGeneration += 1
    drawerDragging.value = true
    drawerRendered.value = true
    drawerOpen.value = gestureMode === 'closing'
  }

  if (event.cancelable) event.preventDefault()
  navigationTranslateX.value = gestureMode === 'opening'
    ? Math.min(0, -drawerWidth.value + dx)
    : Math.max(-drawerWidth.value, dx)
}

function onPageTouchMove(event: TouchEvent) {
  updateTouchGesture(event)
}

async function finishTouchGesture(event: TouchEvent) {
  if (activeTouchId === null || !gestureMode) return
  const touch = findTrackedTouch(event, true)
  if (!touch) return
  const mode = gestureMode
  const dx = touch.clientX - touchStartX
  const elapsed = Math.max(1, performance.now() - touchStartTime)
  const velocity = dx / elapsed
  const crossedDistance = Math.abs(dx) >= drawerWidth.value * SETTLE_RATIO
  const crossedVelocity = Math.abs(velocity) >= FAST_SWIPE_PX_PER_MS
  const shouldOpen = mode === 'opening'
    ? crossedDistance || crossedVelocity
    : !(crossedDistance || crossedVelocity)

  if (!drawerDragging.value) {
    resetTouchGesture()
    return
  }

  const track = getTrackElement()
  const targetX = shouldOpen ? 0 : -drawerWidth.value
  const gestureGeneration = ++drawerGeneration
  resetTouchGesture(false)

  stopDrawerAnimation()
  if (track && !prefersReducedMotion.value) {
    const animation = animate(
      track,
      { x: targetX },
      { duration: 0.24, ease: [0.32, 0.72, 0, 1] },
    )
    drawerAnimation = animation
    await animation
    if (drawerAnimation === animation) drawerAnimation = null
  }

  if (gestureGeneration !== drawerGeneration) return

  drawerOpen.value = shouldOpen
  drawerDragging.value = false
  navigationTranslateX.value = targetX
  if (shouldOpen) {
    void nextTick(() => drawerLinkRefs.value[0]?.$el?.focus())
  } else {
    drawerRendered.value = false
    drawerTrigger.value?.focus()
  }
}

function onPageTouchEnd(event: TouchEvent) {
  void finishTouchGesture(event)
}

function resetTouchGesture(clearDrag = true) {
  activeTouchId = null
  gestureMode = null
  if (clearDrag) drawerDragging.value = false
}

function cancelTouchGesture() {
  if (!drawerDragging.value) {
    resetTouchGesture()
    return
  }
  const wasOpening = gestureMode === 'opening'
  drawerGeneration += 1
  stopDrawerAnimation()
  resetTouchGesture()
  drawerOpen.value = !wasOpening
  navigationTranslateX.value = wasOpening ? -drawerWidth.value : 0
  if (wasOpening) drawerRendered.value = false
}

function onKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape' && drawerRendered.value) closeDrawer()
}

function onFocusIn(event: FocusEvent) {
  if (!drawerInteractive.value || !(event.target instanceof Node)) return
  const panel = getPanelElement()
  if (panel && !panel.contains(event.target)) {
    drawerLinkRefs.value[0]?.$el?.focus()
  }
}

function updateViewportWidth() {
  viewportWidth.value = window.innerWidth
  drawerGeneration += 1
  stopDrawerAnimation()
  resetTouchGesture()

  if (isTablet.value) {
    drawerOpen.value = false
    drawerRendered.value = false
    navigationTranslateX.value = 0
    drawerTrigger.value?.focus()
    return
  }

  navigationTranslateX.value = drawerOpen.value ? 0 : -drawerWidth.value
  if (!drawerOpen.value) drawerRendered.value = false
}

function updateReducedMotion() {
  prefersReducedMotion.value = mediaQuery?.matches ?? false
  if (prefersReducedMotion.value) drawerAnimation?.complete()
}

watch(() => route.path, () => {
  if (drawerRendered.value) closeDrawer()
})

watch([playerOverlayVisible, queueOverlayVisible], ([playerVisible, queueVisible]) => {
  if ((playerVisible || queueVisible) && drawerRendered.value) closeDrawer()
})

onMounted(() => {
  updateViewportWidth()
  mediaQuery = window.matchMedia('(prefers-reduced-motion: reduce)')
  updateReducedMotion()
  mediaQuery.addEventListener('change', updateReducedMotion)
  window.addEventListener('resize', updateViewportWidth)
  window.addEventListener('keydown', onKeydown)
  document.addEventListener('focusin', onFocusIn)
})

onUnmounted(() => {
  stopDrawerAnimation()
  mediaQuery?.removeEventListener('change', updateReducedMotion)
  window.removeEventListener('resize', updateViewportWidth)
  window.removeEventListener('keydown', onKeydown)
  document.removeEventListener('focusin', onFocusIn)
})
</script>

<style scoped lang="scss">
.tabs-layout {
  display: flex;
  flex-direction: column;
  height: 100%;
  box-sizing: border-box;
  touch-action: pan-y;

  &__body {
    position: relative;
    flex: 1;
    min-height: 0;
    overflow: hidden;

    @media (min-width: 768px) {
      height: 100%;
    }
  }

  &__track {
    display: flex;
    width: 150vw;
    height: 100%;
    transform: translateX(-50vw);
    will-change: transform;

    @media (min-width: 768px) {
      width: 100%;
      transform: none !important;
      will-change: auto;
    }
  }

  &__aside {
    display: none;

    @media (min-width: 768px) {
      display: block;
      position: fixed;
      top: 0;
      left: 0;
      bottom: 0;
      z-index: 20;
      width: 260px;
      overflow-y: auto;
      border-right: 1px solid var(--m-hairline);
      background-color: var(--m-surface-1);
      padding-top: var(--m-safe-area-top, 0px);
      box-sizing: border-box;
    }
  }

  &__panel {
    display: flex;
    flex-direction: column;
    min-height: 100%;
    box-sizing: border-box;
  }

  /* 椒盐分段卡片（08-16 三版迭代定案，s2 像素线性扫描）：主/次菜单各为一张圆角卡，
     左 18dp + 右 12dp 空隙（椒盐 x48/x551）、16px 圆角（椒盐 ~15dp）、1px 描边、
     **无阴影**（椒盐实测空隙区纯 #f3f3f3 无投影，08-16 二版 8px24px 阴影系臆造已撤） */
  &__drawer &__nav {
    margin-right: 12px;
    margin-left: 18px;
    background-color: var(--m-surface-1);
    border: 1px solid var(--m-hairline);
    border-radius: 16px;
    padding-top: 8px;
    padding-bottom: 8px;
    overflow: hidden;
  }

  /* 次菜单卡与主菜单卡之间 18dp 空隙（椒盐段间 y1152..1200 = 48px = 18dp）；
     卡片自带描边，去 aside 分组线的 border-top */
  &__drawer &__nav--secondary {
    margin-top: 18px;
    border-top: none;
  }

  &__nav {
    flex: 0 0 auto;
    padding-top: 8px;
  }

  /* 平板 aside 的分组（非卡片形态）：18dp 留白 + hairline */
  &__nav--secondary {
    margin-top: 9px;
    padding-top: 9px;
    border-top: 1px solid var(--m-hairline);
  }

  &__nav-link {
    display: flex;
    align-items: center;
    min-height: 64px;
    padding: 0 var(--m-spacing);
    text-decoration: none;
    font-size: 16px;
    color: var(--m-text);
    border-radius: var(--m-radius-sm);
    box-sizing: border-box;
    transition: background-color 0.15s ease;

    /* 按压瞬时反馈（非常驻高亮；椒盐激活项无蓝底背景） */
    &:active {
      background-color: rgba(var(--m-primary-rgb), 0.08);
    }

    &--active {
      color: var(--m-text);
      font-weight: 600;
      background-color: transparent;
    }
  }

  /* 卡片内菜单项左侧不额外 padding：18px 卡片空隙 + 60px 图标列 → 文字自 ~78px 起，
     对齐椒盐文字 x204px 实测（78px ≈ 204px@2.625） */
  &__drawer &__nav-link {
    padding-left: 0;
  }

  /* 图标区固定 60px（图标 24px 居中于 30px 处，对齐椒盐图标中心 ~x126px） */
  &__nav-icon-shell {
    flex: 0 0 60px;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  /* 图标恒灰色（用户定案：激活项图标不变蓝），激活标识仅文字加粗 */
  &__nav-icon {
    width: var(--m-list-icon);
    height: var(--m-list-icon);
    flex: 0 0 var(--m-list-icon);
    color: var(--m-text-2);
  }

  &__main {
    position: relative;
    flex: 0 0 100vw;
    width: 100vw;
    min-height: 0;

    &--tabbed {
      @media (min-width: 768px) {
        position: fixed;
        top: 0;
        right: 0;
        bottom: 0;
        left: 260px;
        width: auto;
        overflow: hidden;
      }
    }
  }

  /* 抽屉槽位：透明（卡片由 panel 呈现），保持推屏轨道几何与滚动 */
  &__drawer {
    position: relative;
    flex: 0 0 50vw;
    width: 50vw;
    height: 100%;
    padding:
      var(--m-safe-area-top, 0px)
      0
      calc(var(--m-spacing-sub) + var(--m-safe-area-bottom, 0px))
      0;
    background: transparent;
    border: none;
    box-sizing: border-box;
    overflow-y: auto;
    touch-action: pan-y;
    outline: none;

    @media (min-width: 768px) {
      display: none;
    }
  }

  &__drawer-link {
    width: 100%;
    border-radius: var(--m-radius-sm);
  }

  // 透明关闭交互区：承接被推开主页面（inert）的关闭点击。
  // 仅覆盖 50vw 右侧可视区，不遮挡侧栏左滑关闭；无 touch 监听，
  // 左滑手势照常冒泡到 .tabs-layout 处理。层级高于 MNavbar(20)、
  // 低于 MiniPlayer(1000) 与全局弹层。
  &__drawer-dismiss {
    position: fixed;
    top: 0;
    right: 0;
    bottom: 0;
    left: 50vw;
    z-index: 30;
    background: transparent;
  }
}

:global(.dark .tabs-layout__aside) {
  border-right-color: var(--m-hairline);
  background-color: var(--m-surface-1);
}

/* 分段卡片深色：背景/描边走 token 自动切换（--m-surface-1 深 #262626），无阴影无需覆盖 */
</style>
