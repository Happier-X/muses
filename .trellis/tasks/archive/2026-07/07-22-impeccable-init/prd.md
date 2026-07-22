# Impeccable 初始化：PRODUCT.md 与设计上下文

## Goal

为 Muses 建立 Impeccable 项目设计上下文：产出根目录 `PRODUCT.md`、`DESIGN.md`，并在 `AGENTS.md` 增加 Design Context 指针，使后续 `/impeccable *` 与 agent 有一致的产品与视觉依据。

## Requirements

- 注册类型（Register）为 **product**
- 平台（Platform）为 **android**（以 Android 交付为主；视觉非 Material）
- 战略字段经访谈确认后写入 `PRODUCT.md`
- 扫描现网视觉系统并写入 `DESIGN.md` + `.impeccable/design.json`
- 在 `AGENTS.md` 增加 Design Context，指向 `PRODUCT.md` / `DESIGN.md`
- live 模式：平台为 android，跳过浏览器 live 配置

## Acceptance Criteria

- [x] 用户完成战略访谈并确认关键字段
- [x] 根目录存在与确认内容一致的 `PRODUCT.md`（含 Register、Platform 等必填节）
- [x] PRODUCT.md 明确表达「Android 交付 + 非 Material 视觉」的意图
- [x] 已说明 DESIGN.md / live 配置的后续路径或已完成可自动完成的部分
- [x] 给出 2–4 条与 Muses 现状匹配的下一步 Impeccable 命令建议
- [x] 根目录存在扫描生成的 `DESIGN.md` 与 sidecar `.impeccable/design.json`
- [x] `AGENTS.md` 含 Design Context 指针

## Notes

- 轻量任务：文档产物为主。
- North Star：暗场听席；海拔：默认全 flat；组件：沉浸柔和 / 列表干脆；主色：照抄 HeroUI primary `#006FEE`（common.blue 50–900）。
- live：android 平台跳过。
