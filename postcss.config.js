// PostCSS 配置：解包 Tailwind v4 的 @layer，兼容旧版 Android WebView。
// @tailwindcss/vite 处理 Tailwind → PostCSS 处理 @layer 兼容 → Vite 产出。
const layerCompat = {
  postcssPlugin: 'layer-compat',
  AtRule: {
    layer: (atRule) => {
      // @layer xxx { ... } → 解包子节点，保留样式规则
      if (atRule.nodes && atRule.nodes.length > 0) {
        atRule.replaceWith(...atRule.nodes)
      }
    },
  },
}

export default {
  plugins: [layerCompat],
}
