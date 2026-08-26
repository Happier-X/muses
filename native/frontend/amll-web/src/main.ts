/// <reference types="vite/client" />

import '@applemusic-like-lyrics/core/style.css'
import './style.css'
import {
	BackgroundRender,
	LyricPlayer,
	PixiRenderer,
	type LyricLine,
} from '@applemusic-like-lyrics/core'

/** Android 桥接载荷：Kotlin 侧 AmllMapper 序列化后经 evaluateJavascript 注入 */
interface UpdateLyricsPayload {
	/** AMLL LyricLine[]；空数组 = 无歌词（背景照常渲染） */
	lines: LyricLine[]
	/** 封面 URL（appassets https 地址或 data:），null = 沿用上一张（粘性封面由 Kotlin 侧保证） */
	coverUrl: string | null
	/** 歌曲 ID，用于丢弃过期的异步回调 */
	songId: string
}

/**
 * 播放页状态载荷（P4.4 全 WebView 方案）：Kotlin 侧经 window.updatePlayerState 注入。
 * coverUrl=null 表示粘性沿用上一张；title 为空串表示无播放歌曲（前端显示空态）；
 * insetTopPx/insetBottomPx 为状态栏/导航栏避让像素（WebView 内 env() 恒为 0，由原生注入）。
 */
interface PlayerStatePayload {
	title: string
	artist: string | null
	coverUrl: string | null
	isPlaying: boolean
	positionMs: number
	durationMs: number
	buffering: boolean
	repeatMode: 'off' | 'one' | 'all'
	shuffleEnabled: boolean
	hasTranslation: boolean
	translationEnabled: boolean
	insetTopPx?: number
	insetBottomPx?: number
}

/** JS→Native 动作（Kotlin 侧 nativeBridge.onAction 接收后分派 ViewModel） */
type NativeAction =
	| { action: 'playPause' }
	| { action: 'next' }
	| { action: 'previous' }
	| { action: 'seekTo'; positionMs: number }
	| { action: 'setRepeatMode'; mode: 'one' | 'all' }
	| { action: 'setShuffle'; enabled: boolean }
	| { action: 'toggleTranslation' }
	| { action: 'openQueue' }
	| { action: 'close' }

declare global {
	interface Window {
		updateLyrics(payload: string): void
		updatePosition(positionMs: number): void
		pauseRender(): void
		resumeRender(): void
		updatePlayerState(payload: string): void
		nativeBridge?: {
			onAction(json: string): void
		}
	}
}

// ---------- DOM 引用 ----------
function requiredElement<T extends HTMLElement>(id: string): T {
	const el = document.getElementById(id)
	if (!el) throw new Error(`[amll-web] missing element #${id}`)
	return el as T
}

const backgroundLayer = document.getElementById('background-layer')!
const lyricLayer = document.getElementById('lyric-layer')!
const emptyState = document.getElementById('empty-state')!
const playerUi = requiredElement('player-ui')
const panelsEl = requiredElement('pp-panels')
const titleEl = requiredElement('pp-title')
const artistEl = requiredElement('pp-artist')
const coverImg = requiredElement<HTMLImageElement>('pp-cover')
const coverPlaceholder = requiredElement('pp-cover-placeholder')
const metaWindowEl = requiredElement('pp-meta-window')
const metaEmptyEl = requiredElement('pp-meta-empty')
const trackValueEl = requiredElement('pp-track-value')
const timeCurEl = requiredElement('pp-time-cur')
const timeDurEl = requiredElement('pp-time-dur')
const bufferHintEl = requiredElement('pp-buffer-hint')
const btnPrev = requiredElement<HTMLButtonElement>('pp-btn-prev')
const btnPlay = requiredElement<HTMLButtonElement>('pp-btn-play')
const btnNext = requiredElement<HTMLButtonElement>('pp-btn-next')
const btnRepeat = requiredElement<HTMLButtonElement>('pp-btn-repeat')
const btnShuffle = requiredElement<HTMLButtonElement>('pp-btn-shuffle')
const btnQueue = requiredElement<HTMLButtonElement>('pp-btn-queue')
const btnMore = requiredElement<HTMLButtonElement>('pp-btn-more')
const fabTranslate = requiredElement<HTMLButtonElement>('pp-fab-translate')
const fabsContainer = requiredElement('pp-fabs')
const fabPlay = requiredElement<HTMLButtonElement>('pp-fab-play')
const lyricSlot = requiredElement('lyric-slot')
const lyricEmptyEl = requiredElement('pp-lyric-empty')
const progressEl = requiredElement('pp-progress')

// ---------- 内联 SVG 图标（lucide 双轨描边风近似，观感贴近 Web 版） ----------
function svg(inner: string): string {
	return `<svg viewBox="0 0 24 24" aria-hidden="true">${inner}</svg>`
}
const ICONS = {
	play: svg('<polygon points="6 3 20 12 6 21 6 3" />'),
	pause: svg(
		'<rect x="14" y="4" width="4" height="16" rx="1" /><rect x="6" y="4" width="4" height="16" rx="1" />',
	),
	previous: svg('<polygon points="19 20 9 12 19 4 19 20" /><line x1="5" y1="19" x2="5" y2="5" />'),
	next: svg('<polygon points="5 4 15 12 5 20 5 4" /><line x1="19" y1="5" x2="19" y2="19" />'),
	repeat: svg(
		'<path d="m17 2 4 4-4 4" /><path d="M3 11v-1a4 4 0 0 1 4-4h14" /><path d="m7 22-4-4 4-4" /><path d="M21 13v1a4 4 0 0 1-4 4H3" />',
	),
	repeatOne: svg(
		'<path d="m17 2 4 4-4 4" /><path d="M3 11v-1a4 4 0 0 1 4-4h14" /><path d="m7 22-4-4 4-4" /><path d="M21 13v1a4 4 0 0 1-4 4H3" /><path d="M11 10h1v4" />',
	),
	shuffle: svg(
		'<path d="M2 18h1.4c1.3 0 2.5-.6 3.3-1.7l6.1-8.6c.8-1.1 2-1.7 3.3-1.7H22" /><path d="m18 2 4 4-4 4" /><path d="M2 6h1.9c1.5 0 2.9.9 3.6 2.2" /><path d="M22 18h-5.9c-1.3 0-2.6-.7-3.3-1.8l-.5-.8" /><path d="m18 14 4 4-4 4" />',
	),
	queue: svg(
		'<path d="M21 15V6" /><circle cx="18.5" cy="15.5" r="2.5" /><path d="M12 12H3" /><path d="M16 6H3" /><path d="M12 18H3" />',
	),
	more: svg('<circle cx="12" cy="5" r="1" /><circle cx="12" cy="12" r="1" /><circle cx="12" cy="19" r="1" />'),
	translate: svg(
		'<path d="m5 8 6 6" /><path d="m4 14 6-6 2-3" /><path d="M2 5h12" /><path d="M7 2h1" /><path d="m22 22-5-10-5 10" /><path d="M14 18h6" />',
	),
}

btnPrev.innerHTML = ICONS.previous
btnNext.innerHTML = ICONS.next
fabTranslate.innerHTML = ICONS.translate
fabPlay.innerHTML = ICONS.play

// ---------- 背景层（PIXI WebGL 流体渐变） ----------
const canvas = document.createElement('canvas')
Object.assign(canvas.style, { position: 'absolute', inset: '0', width: '100%', height: '100%' } as CSSStyleDeclaration)
backgroundLayer.appendChild(canvas)
const backgroundRender = new BackgroundRender(new PixiRenderer(canvas), canvas)
backgroundRender.setRenderScale(Math.min(window.devicePixelRatio || 1, 2))

// ---------- 歌词层（AMLL LyricPlayer 移入歌词面板槽位，背景保持全屏） ----------
const lyricPlayer = new LyricPlayer()
Object.assign(lyricPlayer.getElement().style, { position: 'absolute', inset: '0', width: '100%', height: '100%' } as CSSStyleDeclaration)
// P4.4：歌词面板成为双面板之一，LyricPlayer 元素挂到面板槽位内（契约不变）
lyricSlot.appendChild(lyricPlayer.getElement())
// 原全屏层不再承担布局职责，仅保留元素引用避免破坏旧契约
lyricLayer.style.display = 'none'

// ---------- 渲染循环 ----------
let paused = false
let lastFrameTime = performance.now()

function frame(now: number) {
	const delta = now - lastFrameTime
	lastFrameTime = now
	if (!paused) {
		// BackgroundRender 由 Pixi 内部 ticker 自驱动；仅歌词需要逐帧 update
		lyricPlayer.update(delta)
	}
	requestAnimationFrame(frame)
}
requestAnimationFrame(frame)

// ---------- 窗口尺寸自适应 ----------
// LyricPlayer 内部用 ResizeObserver 自适应；仅需手动同步背景 canvas 像素尺寸。
// 注意：不能用 window.resize——Android WebView 初始布局时高度为 0（canvas 412x0），
// 后续 AndroidView 获得真实尺寸不派发 resize，背景永远不可见；改用 ResizeObserver。
function resize() {
	backgroundRender.getElement().width = window.innerWidth
	backgroundRender.getElement().height = window.innerHeight
}
window.addEventListener('resize', resize)
new ResizeObserver(resize).observe(document.body)
resize()

// ---------- JS→Native 动作发送（window.nativeBridge 不存在时静默，便于浏览器调试） ----------
function postAction(action: NativeAction): void {
	try {
		window.nativeBridge?.onAction(JSON.stringify(action))
	} catch (err) {
		console.warn('[amll-web] postAction failed', err)
	}
}

// ---------- 工具 ----------
function formatTime(ms: number): string {
	const totalSeconds = Math.max(0, Math.floor(ms / 1000))
	return `${Math.floor(totalSeconds / 60)}:${String(totalSeconds % 60).padStart(2, '0')}`
}

// ---------- 五行歌词小窗（当前行居中，切行整体上移一行） ----------
let lyricLines: LyricLine[] = []
let lastPositionMs = 0
let renderedCurrentIdx = -2 // 已渲染窗口的当前行下标；-2 表示尚未渲染

function lineText(line: LyricLine | undefined): string {
	return line?.words.map((w) => w.word).join('') ?? ''
}

/** 由位置推算当前行下标（无词返回 -1） */
function computeCurrentIdx(positionMs: number): number {
	let idx = -1
	for (let i = 0; i < lyricLines.length; i++) {
		const start = lyricLines[i]?.startTime ?? Number.MAX_SAFE_INTEGER
		if (start <= positionMs) idx = i
		else break
	}
	return idx
}

/** 渲染五行窗口（prev2..next2，恒定五行空行占位防跳动） */
function renderMetaWindow(currentIdx: number): void {
	metaWindowEl.replaceChildren()
	for (let offset = -2; offset <= 2; offset++) {
		const row = document.createElement('p')
		row.className = offset === 0 ? 'pp-meta-line pp-meta-current' : 'pp-meta-line'
		row.textContent = lineText(lyricLines[currentIdx + offset])
		metaWindowEl.appendChild(row)
	}
}

/** 同步五行小窗到最新位置；相邻切行走上移动画，seek/切歌大跳直接换窗 */
function syncMetaWindow(): void {
	if (lyricLines.length === 0) return
	const idx = computeCurrentIdx(lastPositionMs)
	if (idx === renderedCurrentIdx) return
	const adjacent = idx === renderedCurrentIdx + 1 && renderedCurrentIdx >= -1
	renderedCurrentIdx = idx
	if (!adjacent) {
		renderMetaWindow(idx)
		return
	}
	// 相邻切行：先以旧窗口内容下移一行槽位（视觉无跳），再缓动上移回基准，
	// 完成后换新窗口数据（对照 PlayerPage.vue 切行动画 400ms cubic-bezier(.32,.72,0,1)）
	metaWindowEl.style.transition = 'none'
	metaWindowEl.style.transform = 'translateY(0px)'
	void metaWindowEl.offsetWidth // 强制重排使起点生效
	metaWindowEl.style.transition = 'transform 0.4s cubic-bezier(0.32, 0.72, 0, 1)'
	metaWindowEl.style.transform = ''
	window.setTimeout(() => {
		metaWindowEl.style.transition = 'none'
		renderMetaWindow(computeCurrentIdx(lastPositionMs))
	}, 420)
}

// ---------- 歌词面板 chrome（FAB 组）：交互浮现、3s 无操作淡出 ----------
// 对照 Web LYRIC_FAB_IDLE_MS=3000 + revealLyricChrome/scheduleLyricChromeHide
const LYRIC_FAB_IDLE_MS = 3000
let lyricChromeTimer: number | null = null

function scheduleLyricChromeHide(): void {
	if (lyricChromeTimer !== null) window.clearTimeout(lyricChromeTimer)
	lyricChromeTimer = window.setTimeout(() => {
		fabsContainer.classList.remove('is-visible')
		fabsContainer.setAttribute('aria-hidden', 'true')
		lyricChromeTimer = null
	}, LYRIC_FAB_IDLE_MS)
}

function revealLyricChrome(): void {
	if (activePanel !== 1) return
	fabsContainer.classList.add('is-visible')
	fabsContainer.setAttribute('aria-hidden', 'false')
	scheduleLyricChromeHide()
}

function hideLyricChromeImmediate(): void {
	if (lyricChromeTimer !== null) window.clearTimeout(lyricChromeTimer)
	lyricChromeTimer = null
	fabsContainer.classList.remove('is-visible')
}

// ---------- 播放页状态下行 ----------
let playerState: PlayerStatePayload | null = null
let activePanel = 0

function applyPanelsTransform(): void {
	panelsEl.style.transform = `translateX(-${activePanel * 50}%)`
}

function applyTrackAndTime(positionMs: number, durationMs: number): void {
	const fraction = durationMs > 0 ? Math.min(1, Math.max(0, positionMs / durationMs)) : 0
	trackValueEl.style.width = `${fraction * 100}%`
	timeCurEl.textContent = formatTime(positionMs)
}

function applyPlayIcon(): void {
	btnPlay.innerHTML = playerState?.isPlaying ? ICONS.pause : ICONS.play
	fabPlay.innerHTML = playerState?.isPlaying ? ICONS.pause : ICONS.play
	btnPlay.setAttribute('aria-label', playerState?.isPlaying ? '暂停播放' : '播放')
}

window.updatePlayerState = (payload: string) => {
	try {
		const data = JSON.parse(payload) as PlayerStatePayload
		playerState = data
		if (typeof data.insetTopPx === 'number') {
			playerUi.style.setProperty('--pp-safe-top', `${data.insetTopPx}px`)
		}
		if (typeof data.insetBottomPx === 'number') {
			playerUi.style.setProperty('--pp-safe-bottom', `${data.insetBottomPx}px`)
		}

		// 无播放歌曲：显示空态、隐藏整个播放页 UI（手势监听随之失效，安全）
		const hasSong = data.title.length > 0
		playerUi.hidden = !hasSong
		emptyState.hidden = hasSong
		if (!hasSong) return

		titleEl.textContent = data.title
		const artist = data.artist?.trim() ?? ''
		artistEl.textContent = artist
		artistEl.hidden = artist.length === 0

		// 封面：null = 粘性沿用（不清旧图防闪）；有值才更新
		if (data.coverUrl !== null && data.coverUrl !== undefined) {
			coverImg.src = data.coverUrl
			coverImg.hidden = false
			coverPlaceholder.hidden = true
		}

		applyPlayIcon()
		lastPositionMs = data.positionMs
		applyTrackAndTime(data.positionMs, data.durationMs)
		timeDurEl.textContent = data.durationMs > 0 ? formatTime(data.durationMs) : '--:--'
		bufferHintEl.hidden = !data.buffering

		// repeat/shuffle 激活态与图标
		btnRepeat.innerHTML = data.repeatMode === 'one' ? ICONS.repeatOne : ICONS.repeat
		btnRepeat.classList.toggle('pp-active', data.repeatMode !== 'off')
		btnShuffle.classList.toggle('pp-active', data.shuffleEnabled)

		// 歌词面板空态与翻译 FAB
		const hasLyrics = lyricLines.length > 0
		lyricEmptyEl.hidden = hasLyrics
		fabTranslate.hidden = !data.hasTranslation
	} catch (err) {
		console.error('[amll-web] updatePlayerState parse failed', err)
	}
}

// ---------- 桥接实现（歌词载荷通道沿用既有契约） ----------
let currentSongId = ''

window.updateLyrics = (payload: string) => {
	try {
		const data = JSON.parse(payload) as UpdateLyricsPayload
		currentSongId = data.songId

		lyricLines = Array.isArray(data.lines) ? [...data.lines] : []
		lyricPlayer.setLyricLines(data.lines ?? [], 0)
		backgroundRender.setHasLyric(lyricLines.length > 0)

		if (lyricLines.length === 0) {
			// 无词：歌词面板空态 + 五行小窗占位提示「暂无歌词」；背景照常渲染
			metaWindowEl.replaceChildren()
			metaEmptyEl.hidden = false
			renderedCurrentIdx = -2
		} else {
			metaEmptyEl.hidden = true
			renderedCurrentIdx = -2
			syncMetaWindow()
		}

		// 封面：null 表示 Kotlin 侧粘性沿用，不主动清空避免闪回 fallback 背景
		if (data.coverUrl !== null) {
			void backgroundRender.setAlbum(data.coverUrl).catch((err) => {
				console.warn('[amll-web] setAlbum failed', err)
			})
		}
	} catch (err) {
		console.error('[amll-web] updateLyrics parse failed', err)
	}
}

window.updatePosition = (positionMs: number) => {
	lyricPlayer.setCurrentTime(positionMs)
	lastPositionMs = positionMs
	syncMetaWindow()
}

window.pauseRender = () => {
	paused = true
	backgroundRender.pause()
}

window.resumeRender = () => {
	lastFrameTime = performance.now()
	paused = false
	backgroundRender.resume()
}

// ---------- 控制键 / mode-bar 点击 → nativeBridge.onAction ----------
btnPlay.addEventListener('click', () => postAction({ action: 'playPause' }))
btnPrev.addEventListener('click', () => postAction({ action: 'previous' }))
btnNext.addEventListener('click', () => postAction({ action: 'next' }))
btnQueue.addEventListener('click', () => postAction({ action: 'openQueue' }))
// 「更多」仅入口占位：编辑歌曲信息等动作单属 M3 范围
btnMore.addEventListener('click', () => undefined)
fabTranslate.addEventListener('click', () => postAction({ action: 'toggleTranslation' }))
// 对齐 Vue onToggleRepeat 语义：one ↔ all 二态切换
btnRepeat.addEventListener('click', () => {
	const next = playerState?.repeatMode === 'one' ? 'all' : 'one'
	postAction({ action: 'setRepeatMode', mode: next })
})
btnShuffle.addEventListener('click', () =>
	postAction({ action: 'setShuffle', enabled: !(playerState?.shuffleEnabled ?? false) }),
)

// ---------- 进度条拖动（自绘无 thumb：本地 preview 跟手，抬起一次性 seekTo） ----------
let seekPreviewFraction: number | null = null

progressEl.addEventListener('pointerdown', (e) => {
	const duration = playerState?.durationMs ?? 0
	if (duration <= 0) return
	progressEl.setPointerCapture(e.pointerId)
	seekPreviewFraction = Math.min(1, Math.max(0, e.clientX / progressEl.clientWidth))
	trackValueEl.style.width = `${seekPreviewFraction * 100}%`
})
progressEl.addEventListener('pointermove', (e) => {
	if (seekPreviewFraction === null) return
	seekPreviewFraction = Math.min(1, Math.max(0, e.clientX / progressEl.clientWidth))
	trackValueEl.style.width = `${seekPreviewFraction * 100}%`
})
function endSeekPreview(commit: boolean): void {
	const fraction = seekPreviewFraction
	seekPreviewFraction = null
	if (fraction === null || !commit) return
	const duration = playerState?.durationMs ?? 0
	if (duration <= 0) return
	postAction({ action: 'seekTo', positionMs: Math.round(fraction * duration) })
}
progressEl.addEventListener('pointerup', () => endSeekPreview(true))
progressEl.addEventListener('pointercancel', () => endSeekPreview(false))

// ---------- 手势系统（方向锁定互斥：竖直下拉关闭 / 横滑切面板） ----------
interface TouchSession {
	startX: number
	startY: number
	direction: 'none' | 'vertical' | 'horizontal'
	dy: number
	dx: number
}
let touchSession: TouchSession | null = null

/** 下拉关闭阈值（px）：任务定案 >120 触发关闭 */
const CLOSE_THRESHOLD_PX = 120
/** 方向判定阈值（Vue Math.max(|dx|,|dy|) > 8） */
const DIRECTION_SLOP_PX = 8
/** 横滑切面板阈值：>40% 视口宽（任务定案） */
function swipeThresholdPx(): number {
	return window.innerWidth * 0.4
}

playerUi.addEventListener(
	'touchstart',
	(e) => {
		// 进度条区域自管拖动（onProgressGestureStart 隔离语义）：不启动 overlay 手势
		const target = e.target instanceof Element ? e.target : null
		if (target?.closest('#pp-progress')) {
			touchSession = null
			return
		}
		const t = e.changedTouches[0]
		if (!t) return
		touchSession = { startX: t.clientX, startY: t.clientY, direction: 'none', dy: 0, dx: 0 }
	},
	{ passive: true },
)

playerUi.addEventListener(
	'touchmove',
	(e) => {
		const s = touchSession
		const t = e.changedTouches[0]
		if (!s || !t) return
		e.preventDefault() // 其余区域一律拦截默认滚动，防止穿透

		s.dx = t.clientX - s.startX
		s.dy = t.clientY - s.startY
		if (s.direction === 'none' && Math.max(Math.abs(s.dx), Math.abs(s.dy)) > DIRECTION_SLOP_PX) {
			// 方向锁定互斥；竖直下拉仅在 info 面板（activePanel===0）可用
			s.direction = Math.abs(s.dy) > Math.abs(s.dx) ? 'vertical' : 'horizontal'
			if (s.direction === 'vertical') {
				playerUi.style.transition = 'none'
			}
		}

		if (s.direction === 'vertical' && activePanel === 0) {
			playerUi.style.transform = `translateY(${Math.max(0, s.dy)}px)` // 跟手下拉
		}
	},
	{ passive: false },
)

function finishTouch(cancelled: boolean): void {
	const s = touchSession
	touchSession = null
	if (!s || s.direction === 'none') return

	if (s.direction === 'vertical') {
		if (activePanel !== 0 || cancelled) {
			playerUi.style.transform = ''
			playerUi.style.transition = ''
			return
		}
		if (s.dy >= CLOSE_THRESHOLD_PX) {
			postAction({ action: 'close' }) // 超阈值收起关闭播放页
			return
		}
		// 未过阈值：220ms easeOut 回弹归位
		playerUi.style.transition = 'transform 0.22s ease-out'
		playerUi.style.transform = ''
		window.setTimeout(() => {
			playerUi.style.transition = ''
		}, 240)
		return
	}

	// horizontal：位移 >40% 视口宽切面板（endX < startX → 歌词面板）
	if (!cancelled && Math.abs(s.dx) > swipeThresholdPx()) {
		activePanel = s.dx < 0 ? 1 : 0
		applyPanelsTransform()
		if (activePanel !== 1) hideLyricChromeImmediate()
	}
}

// 歌词面板交互（触摸抬起/滚动）→ 浮现 FAB 组并重置 3s 计时；
// 点 FAB 自身不经过这里（click.stop 语义由 target 过滤近似——touchend 在 fab 上时跳过）
lyricSlot.addEventListener('touchend', () => revealLyricChrome(), { passive: true })
lyricSlot.addEventListener('scroll', () => revealLyricChrome(), { passive: true })
fabTranslate.addEventListener('click', () => scheduleLyricChromeHide())
fabPlay.addEventListener('click', () => scheduleLyricChromeHide())

playerUi.addEventListener('touchend', () => finishTouch(false))
playerUi.addEventListener('touchcancel', () => finishTouch(true))

export { currentSongId }
