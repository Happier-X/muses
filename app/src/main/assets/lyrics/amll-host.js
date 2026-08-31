/*
 * AMLL 网页版宿主桥：包装 @applemusic-like-lyrics/core 的 LyricPlayer，
 * 暴露给原生 WebView 调用的 AMLLHost 接口，并把行点击/触摸交互转发给原生。
 * 构建：esbuild amll-host.js --bundle --minify --format=iife --outfile=amll.bundle.js
 */
import { LyricPlayer } from "@applemusic-like-lyrics/core";

const root = document.getElementById("player");
console.log("AMLL host: boot, root=", root);

let player;
try {
	player = new LyricPlayer(root);
	console.log("AMLL host: LyricPlayer created");
} catch (e) {
	console.error("AMLL host: LyricPlayer create FAILED", e);
	// 构造失败则抛出让脚本中断，便于原生日志定位
	throw e;
}

// 原生通过 evaluateJavascript 调用的桥
window.AMLLHost = {
	/** linesJson: LyricLine[] 的 JSON 字符串；timeMs: 当前播放位置 */
	setLyric: (linesJson, timeMs) => {
		try {
			const lines = linesJson ? JSON.parse(linesJson) : [];
			player.setLyricLines(lines, timeMs | 0);
			player.update(16);
		} catch (e) {
			console.error("AMLLHost.setLyric", e);
		}
	},
	setCurrentTime: (ms, isSeek) => {
		player.setCurrentTime(ms | 0, !!isSeek);
		player.update(16);
	},
	setPlaying: (playing) => {
		if (playing) {
			player.resume();
		} else {
			player.pause();
		}
		player.update(16);
	},
	setFontSize: (px) => {
		root.style.fontSize = px + "px";
	},
	clearLyric: () => {
		player.setLyricLines([]);
		player.update(16);
	},
};
console.log("AMLL host: AMLLHost ready");

// 点击歌词行 → seek（转发给原生）
player.addEventListener("line-click", (evt) => {
	if (!window.AndroidLyric) return;
	if (typeof evt.lineIndex !== "number" || evt.lineIndex < 0) return;
	try {
		const lines = player.getLyricLines();
		const line = lines[evt.lineIndex];
		if (line && typeof line.startTime === "number") {
			window.AndroidLyric.onSeek(line.startTime);
		}
	} catch (e) {
		console.error("line-click", e);
	}
});

// 触摸交互 → 原生（用于唤起控制条 chrome）
root.addEventListener(
	"touchstart",
	() => {
		if (window.AndroidLyric) window.AndroidLyric.onInteractionStart();
	},
	{ passive: true },
);
root.addEventListener(
	"touchend",
	() => {
		if (window.AndroidLyric) window.AndroidLyric.onInteractionEnd();
	},
	{ passive: true },
);

window.AMLLReady = true;
console.log("AMLL host: ready");
