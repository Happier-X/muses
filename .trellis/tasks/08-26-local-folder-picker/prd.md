# 修复添加本地文件夹——接入系统目录选择器

## 现状
音源页「添加本地文件夹」仅打开手填表单（要求输入 SAF tree URI 字符串），普通用户无法使用。
Web 版语义：FilePicker.pickDirectory 拉起系统选择器 → 选完即建源。

## 方案
1. SourcesScreen 用 `ActivityResultContracts.OpenDocumentTree` 拉起系统目录选择器
2. `takePersistableUriPermission` 持久化访问权
3. 解析 tree/document id → 物理绝对路径前缀（primary:Music → /storage/emulated/0/Music；
   SD 卡卷 XXXX-XXXX:dir → /storage/XXXX-XXXX/dir），Source.path 存物理前缀——
   现有 LocalLibraryScanner 的 MediaStore DATA 前缀过滤直接可用，扫描器零改动
4. 选完即建源（名称=目录显示名）并 toast 提示；AddSourceSheet 本地手填表单保留作高级入口

## Acceptance Criteria
- [ ] 点击「添加本地文件夹」拉起系统目录选择器
- [ ] 选择后音源卡片出现且可正常扫描入库

## 验证记录（2026-08-26）
- ✅ 点击「添加本地文件夹」成功拉起系统目录选择器（SAF DocumentsUI）
- ⚠️ MuMu 模拟器 ROM 将内部存储目录标记为「无法使用此文件夹（隐私保护）」，无法在
  模拟器上完成端到端选择；此为 DocumentsUI 系统行为而非应用问题，需真机验证
