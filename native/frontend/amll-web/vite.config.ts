import { defineConfig } from 'vite'
import wasm from 'vite-plugin-wasm'
import topLevelAwait from 'vite-plugin-top-level-await'

// AMLL WebView 前端：产物直接输出到 feature:player 的 androidAssets，
// 由 Android WebViewAssetLoader 以 appassets.androidplatform.net 加载。
export default defineConfig({
	// 相对路径：资产必须能在 https://appassets.androidplatform.net/assets/amll/ 下正确解析
	base: './',
	plugins: [wasm(), topLevelAwait()],
	build: {
		outDir: '../../feature/player/src/main/androidAssets/amll',
		emptyOutDir: true,
		target: 'es2022',
	},
})
