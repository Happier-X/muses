/// <reference types="vite/client" />

import '@applemusic-like-lyrics/core/style.css'
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

declare global {
	interface Window {
		updateLyrics(payload: string): void
		updatePosition(positionMs: number): void
		pauseRender(): void
		resumeRender(): void
	}
}

const backgroundLayer = document.getElementById('background-layer')!
const lyricLayer = document.getElementById('lyric-layer')!
const emptyState = document.getElementById('empty-state')!

// ---------- 背景层（PIXI WebGL 流体渐变） ----------
const canvas = document.createElement('canvas')
Object.assign(canvas.style, { position: 'absolute', inset: '0', width: '100%', height: '100%' } as CSSStyleDeclaration)
backgroundLayer.appendChild(canvas)
const backgroundRender = new BackgroundRender(new PixiRenderer(canvas), canvas)
backgroundRender.setRenderScale(Math.min(window.devicePixelRatio || 1, 2))

// ---------- 歌词层 ----------
const lyricPlayer = new LyricPlayer()
Object.assign(lyricPlayer.getElement().style, { position: 'absolute', inset: '0', width: '100%', height: '100%' } as CSSStyleDeclaration)
lyricLayer.appendChild(lyricPlayer.getElement())

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
// LyricPlayer 内部用 ResizeObserver 自适应；仅需手动同步背景 canvas 像素尺寸
function resize() {
	backgroundRender.getElement().width = window.innerWidth
	backgroundRender.getElement().height = window.innerHeight
}
window.addEventListener('resize', resize)
resize()

// ---------- 桥接实现 ----------
let currentSongId = ''

window.updateLyrics = (payload: string) => {
	try {
		const data = JSON.parse(payload) as UpdateLyricsPayload
		currentSongId = data.songId

		const lines = Array.isArray(data.lines) ? data.lines : []
		lyricPlayer.setLyricLines(lines, 0)
		backgroundRender.setHasLyric(lines.length > 0)

		if (lines.length === 0) {
			emptyState.hidden = false
			lyricLayer.style.visibility = 'hidden'
		} else {
			emptyState.hidden = true
			lyricLayer.style.visibility = 'visible'
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

export { currentSongId }
