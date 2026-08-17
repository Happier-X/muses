# 执行计划

## 顺序清单

1. [x] 用例固化进 vitest：`tests/unit/mergeTranslation.spec.ts`（13 用例）+ `package.json` 新增
   devDependency `vitest ^4.1.10` + `test:unit` 脚本（spec quality-guidelines 声称 vitest
   在技术栈内但仓库此前未装——本次恢复最小 vitest 供 lyrics 纯函数回归，属决策留痕：
   仓库 6043084 曾刻意 drop tests，本次仅恢复 vitest，不装 jsdom/cypress）。
2. [x] `mergeTranslation.ts`：交替结构感知配对（detectShiftedTranslationPairs +
   applyShiftedTranslationPairs）+ 结构一致性 + 时间定义属性校验，同时间戳相邻配对回退。
3. [x] yrc+tlyric 容差：序列感知回退匹配（alignTranslationSequence + 边界防误判）。
4. [x] 回归 + 真实平台数据抽查（Faded lrc/yrc + tlyric 均 54/58 正确挂载）。
5. [x] `npm run lint`（src+tests exit 0）+ `npm run test:unit`（13/13）+ `npm run build`（exit 0）。

## 质量检查（trellis-check 两轮）

- 第一轮发现 4 处建议级 + 1 覆盖担忧，全部处理：
  ① shifted 路径残留 aligned 对不合并（混合结构）→ shifted 后再跑同时间戳相邻合并；
  ② 序列回退复用已消费 stamp → 首遍循环内精确记录 consumedStamps；
  ③ pending 过滤缺 isBG → 补上；④ wordsText/linePlainText 重复 helper → 合并为 linePlainText；
  ⑤ 用例固化 vitest（见上）。
- 第二轮发现 2 个确认风险，全部修复 + 新增对抗测试（l/m）：
  F1 垫词行打断交替结构 → 增加的**结构一致性**（配对主行同脚本族）+
  **时间定义属性**（译文时间戳贴近下一窗口原文行时间戳，cross-window）校验，拒绝激活；
  F2 tlyric 自身错移一句 → 序列回退**边界防误判**（首行无译 + 末 stamp 无承接 → 放弃）。

## 验证命令

- `npm run test:unit`（vitest，13 用例，含 bug 验收 a/b + 回归 c..k + 对抗 l/m）。
- `npm run lint`（src+tests exit 0）/ `npm run build`（vue-tsc + vite，exit 0）。

## 审查门 / 回滚点

- [x] 步骤 2 完成后：「同时间独立两句不被吞」「零星中文不误配」用例均绿。
- [x] 对抗用例 l（垫词打断）与 m（tlyric 错移）绿。
- 回滚：`git revert` 单提交即可（改动仅 mergeTranslation.ts + 测试 + vitest devDep）。
