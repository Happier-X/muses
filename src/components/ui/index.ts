/** 组件出口：Konsta UI v5（iOS 主题）k* 组件 re-export + 自建组件收编。 */

/** app-only：音乐封面 */
export { default as MCover } from './MCover.vue'
/** app-only：iOS 风格空状态（Konsta 无 empty 组件） */
export { default as MEmpty } from './MEmpty.vue'
/** 自定义页壳 */
export { default as MPage } from './MPage.vue'
export { default as MContent } from './MContent.vue'

/** Konsta UI v5：k* 组件 re-export，供页面按需引入 */
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
