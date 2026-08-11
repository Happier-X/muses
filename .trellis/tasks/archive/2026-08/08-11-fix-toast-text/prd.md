# PRD：修复 toast 文字白色不可见

## 背景

用户报告：toast 里的文字是白色的，看不出来。

## 根因（CDP 实测确认）

1. **text 根因**：项目 `tailwind.css` 里 `body { color: var(--color-ios-light-surface-1) }`，而 `--color-ios-light-surface-1 = #ffffff`（浅色模式）→ **body 默认文字色为白色**。Konsta `k-toast` iOS 主题 `textIos` 为空（ToastColors.js），toast 文字无显式颜色 → **继承 body 白色**；toast 玻璃底是半透明白（bg-ios-light-glass rgba(255,255,255,0.75)）→ **白字白玻璃 = 不可见**。深色模式正常（深玻璃 + 白字）。

2. **连带发现（非用户报告）**：k-glass 的 `backdrop-blur-lg` 在 WebView 110 上 computed `backdrop-filter: none`（CSS 规则存在、元素匹配、--blur-lg 变量已定义，但 var() 链规则未生效——与 navbar 的 backdrop-blur-[2px] 同类问题，navbar 已单点修复）。MiniPlayer 玻璃胶囊等用 backdrop-blur-lg 的地方同样受影响。

## 修复内容（src/theme/tailwind.css 追加）

```css
.k-toast {
  color: #000000;
}
.dark .k-toast {
  color: #ffffff;
}

.backdrop-blur-lg {
  -webkit-backdrop-filter: blur(16px);
  backdrop-filter: blur(16px);
}
```

- toast 文字显式设色：浅色黑字（白玻璃）、深色白字（深玻璃）——与 Konsta 其他组件 text-black dark:text-white 惯例一致
- backdrop-blur-lg 直接给值覆盖 Tailwind var() 链规则（navbar 先例），MiniPlayer / k-glass / tones 全受益

## 验收

- [x] 浅色模式 .k-toast computed color = rgb(0,0,0)（黑字白玻璃）
- [x] 深色模式 .k-toast computed color = rgb(255,255,255)（白字深玻璃 rgba(50,50,50,.5)）
- [x] .k-glass / MiniPlayer（backdrop-blur-lg 元素）computed backdrop-filter = blur(16px)
- [x] 文字颜色为确定性继承（color 继承链验证），三处 k-toast 同规则自动生效
- [x] npm run build 通过