// 跨组件共享的 action sheet 按钮样式（纯 Tailwind utility 组合）
// SongsPage、PlaylistsPage、SourcesPage 等均使用相同的类组合以保证视觉一致

export const actionSheetItemClass =
  'action-sheet-item flex items-center justify-center w-full min-h-[var(--h-touch-target,48px)] p-[var(--h-space-md,12px)] border-none rounded-[var(--h-radius-control,12px)] bg-transparent text-[var(--h-color-ink,#000)] [font-family:inherit] [font-weight:inherit] [font-style:inherit] text-[length:var(--h-font-title,15px)] cursor-pointer [-webkit-tap-highlight-color:transparent] transition-[background-color] duration-[var(--h-duration-press,0.12s)] ease-[ease] active:bg-[var(--h-color-surface-secondary,#f4f4f5)]'

export const actionSheetCancelClass =
  'action-sheet-item action-cancel font-medium text-[var(--h-color-primary,#006fee)]'

export const actionSheetDestructiveClass =
  'action-sheet-item action-destructive text-[var(--h-color-danger,#f31260)]'