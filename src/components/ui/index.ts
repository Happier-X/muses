/** 组件出口：自研 m-* 组件集（Konsta iOS 主题视觉，无需组件库/Tailwind）。 */

/** app-only：音乐封面及稳定占位 */
export { default as MCover } from './MCover.vue'
/** app-only：iOS 风格空状态 */
export { default as MEmpty } from './MEmpty.vue'

/** 基础 */
export { default as MButton } from './MButton.vue'
export { default as MBlockTitle } from './MBlockTitle.vue'
export { default as MCard } from './MCard.vue'
export { default as MFab } from './MFab.vue'
export { default as MPreloader } from './MPreloader.vue'

/** 表单 */
export { default as MCheckbox } from './MCheckbox.vue'
export { default as MToggle } from './MToggle.vue'
export { default as MRange } from './MRange.vue'
export { default as MListInput } from './MListInput.vue'

/** 列表 */
export { default as MList } from './MList.vue'
export { default as MListItem } from './MListItem.vue'

/** 导航 */
export { default as MNavbar } from './MNavbar.vue'
export { default as MNavbarBackLink } from './MNavbarBackLink.vue'

/** 分段 */
export { default as MSegmented } from './MSegmented.vue'
export { default as MSegmentedButton } from './MSegmentedButton.vue'

/** Tabbar */
export { default as MTabbar } from './MTabbar.vue'
export { default as MTabbarLink } from './MTabbarLink.vue'

/** 浮层 */
export { default as MActions } from './MActions.vue'
export { default as MActionsGroup } from './MActionsGroup.vue'
export { default as MActionsLabel } from './MActionsLabel.vue'
export { default as MActionsButton } from './MActionsButton.vue'
export { default as MDialog } from './MDialog.vue'
export { default as MDialogButton } from './MDialogButton.vue'
export { default as MSheet } from './MSheet.vue'
export { default as MPopup } from './MPopup.vue'
export { default as MToast } from './MToast.vue'

/** Konsta UI 兼容 re-export（页面迁移期临时保留，全部迁移后移除） */
export {
  kActions,
  kActionsButton,
  kActionsGroup,
  kActionsLabel,
  kApp,
  kBlock,
  kBlockFooter,
  kBlockHeader,
  kBlockTitle,
  kButton,
  kCard,
  kCheckbox,
  kChip,
  kDialog,
  kDialogButton,
  kFab,
  kGlass,
  kIcon,
  kLink,
  kList,
  kListButton,
  kListGroup,
  kListInput,
  kListItem,
  kNavbar,
  kNavbarBackLink,
  kPage,
  kPopover,
  kPopup,
  kPreloader,
  kProgressbar,
  kRadio,
  kRange,
  kSearchbar,
  kSegmented,
  kSegmentedButton,
  kSheet,
  kStepper,
  kTabbar,
  kTabbarLink,
  kTable,
  kTableBody,
  kTableCell,
  kTableHead,
  kTableRow,
  kToast,
  kToggle,
  kToolbar,
  kToolbarPane,
  kProvider,
} from 'konsta/vue'