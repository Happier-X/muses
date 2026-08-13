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
        <nav aria-label="主导航">
          <RouterLink
            v-for="item in navItems"
            :key="item.to"
            :to="item.to"
            class="tabs-layout__nav-link"
            :class="{ 'tabs-layout__nav-link--active': isNavActive(item) }"
            :aria-current="isNavActive(item) ? 'page' : undefined"
          >
            <component :is="item.icon" aria-hidden="true" class="tabs-layout__nav-icon" />
            <span>{{ item.label }}</span>
          </RouterLink>
        </nav>
      </aside>

      <main
        class="tabs-layout__main"
        :class="{ 'tabs-layout__main--tabbed': isTabsRoute }"
        :inert="drawerInteractive || undefined"
        :aria-hidden="drawerInteractive ? 'true' : undefined"
      >
        <RouterView />
      </main>
    </div>

    <AnimatePresence @exit-complete="onDrawerExitComplete">
      <template v-if="drawerRendered && !isTablet && isTabsRoute">
        <motion.div
          key="navigation-backdrop"
          class="tabs-layout__backdrop"
          :initial="{ opacity: 0 }"
          :animate="{ opacity: backdropOpacity }"
          :exit="{ opacity: 0 }"
          :transition="drawerTransition"
          aria-hidden="true"
          @click.stop="closeDrawer"
        />
        <motion.aside
          ref="drawerPanelRef"
          key="navigation-drawer"
          class="tabs-layout__drawer"
          :initial="{ x: '-100%' }"
          :animate="{ x: drawerTranslateX }"
          :exit="{ x: '-100%' }"
          :transition="drawerTransition"
          role="dialog"
          aria-modal="true"
          aria-label="主导航"
          @touchstart.stop="onDrawerTouchStart"
          @touchmove.stop="onDrawerTouchMove"
          @touchend.stop="onDrawerTouchEnd"
          @touchcancel.stop="cancelTouchGesture"
        >
          <nav aria-label="主导航">
            <RouterLink
              v-for="item in navItems"
              :key="item.to"
              ref="drawerLinkRefs"
              :to="item.to"
              class="tabs-layout__nav-link tabs-layout__drawer-link"
              :class="{ 'tabs-layout__nav-link--active': isNavActive(item) }"
              :aria-current="isNavActive(item) ? 'page' : undefined"
              @click="onDrawerNavigation"
            >
              <component :is="item.icon" aria-hidden="true" class="tabs-layout__nav-icon" />
              <span>{{ item.label }}</span>
            </RouterLink>
          </nav>
        </motion.aside>
      </template>
    </AnimatePresence>
  </div>
</template>

<script setup lang="ts">
import { AnimatePresence, animate, motion } from 'motion-v'
import { computed, nextTick, onMounted, onUnmounted, provide, readonly, ref, watch } from 'vue'
import { RouterLink, RouterView, useRoute } from 'vue-router'
import type { ComponentPublicInstance } from 'vue'
import { library, musicalNotes, radio, settings } from '@/icons'
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
  {
    to: '/tabs/categories',
    label: '音乐库',
    icon: library,
    childPrefixes: ['/tabs/playlists/', '/tabs/library/'],
  },
  { to: '/tabs/sources', label: '音源', icon: radio },
  { to: '/tabs/settings', label: '设置', icon: settings },
]

const HORIZONTAL_LOCK_PX = 8
const SETTLE_RATIO = 0.25
const FAST_SWIPE_PX_PER_MS = 0.5
const route = useRoute()
const viewportWidth = ref(typeof window === 'undefined' ? 0 : window.innerWidth)
const drawerOpen = ref(false)
const drawerRendered = ref(false)
const drawerDragging = ref(false)
const drawerTranslateX = ref(0)
const drawerPanelRef = ref<ComponentPublicInstance | HTMLElement | null>(null)
const drawerLinkRefs = ref<ComponentPublicInstance[]>([])
const drawerTrigger = ref<HTMLElement | null>(null)
const prefersReducedMotion = ref(false)
const isTablet = computed(() => viewportWidth.value >= 768)
const isTabsRoute = computed(() => route.path === '/tabs' || route.path.startsWith('/tabs/'))
const drawerInteractive = computed(() => drawerOpen.value || drawerDragging.value)
const drawerWidth = computed(() => Math.min(300, viewportWidth.value * 0.82))
const backdropOpacity = computed(() => {
  if (drawerOpen.value && !drawerDragging.value) return 1
  return Math.max(0, Math.min(1, 1 + drawerTranslateX.value / drawerWidth.value))
})
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
let mediaQuery: MediaQueryList | null = null

provide(navigationDrawerKey, {
  expanded: readonly(drawerOpen),
  open: openDrawer,
})

const isNavActive = (item: NavigationItem) => {
  if (route.path === item.to || route.path.startsWith(`${item.to}/`)) return true
  return item.childPrefixes?.some((prefix) => route.path.startsWith(prefix)) ?? false
}

function getPanelElement() {
  const panel = drawerPanelRef.value
  if (!panel) return null
  return panel instanceof HTMLElement ? panel : panel.$el as HTMLElement
}

function openDrawer(trigger?: HTMLElement | null) {
  if (isTablet.value || !isTabsRoute.value || playerOverlayVisible.value || queueOverlayVisible.value) return
  drawerGeneration += 1
  drawerTrigger.value = trigger ?? drawerTrigger.value
  drawerRendered.value = true
  drawerOpen.value = true
  drawerDragging.value = false
  drawerTranslateX.value = 0
  void nextTick(() => drawerLinkRefs.value[0]?.$el?.focus())
}

function closeDrawer() {
  if (!drawerRendered.value) return
  drawerGeneration += 1
  resetTouchGesture()
  drawerOpen.value = false
  drawerRendered.value = false
}

function onDrawerExitComplete() {
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
  if (isTablet.value || !isTabsRoute.value || drawerRendered.value || playerOverlayVisible.value || queueOverlayVisible.value) return
  beginTouchGesture(event, 'opening')
}

function onDrawerTouchStart(event: TouchEvent) {
  if (!drawerOpen.value) return
  beginTouchGesture(event, 'closing')
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

    drawerDragging.value = true
    drawerRendered.value = true
    drawerOpen.value = gestureMode === 'closing'
  }

  if (event.cancelable) event.preventDefault()
  drawerTranslateX.value = gestureMode === 'opening'
    ? Math.min(0, -drawerWidth.value + dx)
    : Math.max(-drawerWidth.value, dx)
}

function onPageTouchMove(event: TouchEvent) {
  updateTouchGesture(event)
}

function onDrawerTouchMove(event: TouchEvent) {
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

  const panel = getPanelElement()
  const targetX = shouldOpen ? 0 : -drawerWidth.value
  const gestureGeneration = ++drawerGeneration
  resetTouchGesture(false)

  if (panel && !prefersReducedMotion.value) {
    await animate(
      panel,
      { transform: `translateX(${targetX}px)` },
      { duration: 0.24, ease: [0.32, 0.72, 0, 1] },
    )
  }

  if (gestureGeneration !== drawerGeneration) return

  drawerOpen.value = shouldOpen
  drawerDragging.value = false
  drawerTranslateX.value = targetX
  if (shouldOpen) {
    void nextTick(() => drawerLinkRefs.value[0]?.$el?.focus())
  } else {
    drawerRendered.value = false
  }
}

function onPageTouchEnd(event: TouchEvent) {
  void finishTouchGesture(event)
}

function onDrawerTouchEnd(event: TouchEvent) {
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
  resetTouchGesture()
  drawerOpen.value = !wasOpening
  drawerTranslateX.value = wasOpening ? -drawerWidth.value : 0
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
  if (isTablet.value) closeDrawer()
}

function updateReducedMotion() {
  prefersReducedMotion.value = mediaQuery?.matches ?? false
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
    display: flex;
    flex-direction: column;
    flex: 1;
    min-height: 0;

    @media (min-width: 768px) {
      display: block;
      height: 100%;
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
      padding-top: calc(var(--m-spacing-sub) + var(--m-safe-area-top, 0px));
      box-sizing: border-box;
    }
  }

  &__nav-link {
    display: flex;
    align-items: center;
    gap: var(--m-spacing-sub);
    min-height: var(--m-list-row-h);
    padding: 0 var(--m-spacing);
    text-decoration: none;
    font-size: 15px;
    color: var(--m-text);
    box-sizing: border-box;

    &--active {
      color: var(--m-primary);
      font-weight: 600;
      background-color: rgba(var(--m-primary-rgb), 0.12);
    }
  }

  &__nav-icon {
    width: var(--m-list-icon);
    height: var(--m-list-icon);
    flex: 0 0 var(--m-list-icon);
  }

  &__main {
    position: relative;
    flex: 1;
    min-height: 0;

    &--tabbed {
      @media (min-width: 768px) {
        position: fixed;
        top: 0;
        right: 0;
        bottom: 0;
        left: 260px;
        overflow: hidden;
      }
    }
  }

  &__backdrop {
    position: fixed;
    inset: 0;
    z-index: 1050;
    background-color: rgba(0, 0, 0, 0.45);
    touch-action: none;
  }

  &__drawer {
    position: fixed;
    top: 0;
    bottom: 0;
    left: 0;
    z-index: 1050;
    width: min(300px, 82vw);
    padding:
      calc(var(--m-spacing-sub) + var(--m-safe-area-top, 0px))
      var(--m-safe-area-right, 0px)
      calc(var(--m-spacing-sub) + var(--m-safe-area-bottom, 0px))
      var(--m-safe-area-left, 0px);
    border-right: 1px solid var(--m-hairline);
    background-color: var(--m-surface-1);
    box-sizing: border-box;
    overflow-y: auto;
    touch-action: pan-y;
    outline: none;
  }

  &__drawer-link {
    width: 100%;
    border-radius: var(--m-radius-sm);
  }
}

:global(.dark) .tabs-layout__aside,
:global(.dark) .tabs-layout__drawer {
  border-right-color: var(--m-hairline);
  background-color: var(--m-surface-1);
}
</style>
