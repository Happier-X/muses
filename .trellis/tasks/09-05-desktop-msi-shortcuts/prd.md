# 桌面 MSI 补 Windows 快捷方式与固定升级 UUID

## Goal

composeApp 打包配置增加 windows.menu/menuGroup/shortcut/dirChooser,并为 MSI 固定 upgradeUuid,使安装后有开始菜单/桌面入口且后续版本可覆盖升级;同步更新 ci 发包契约 spec。

## 背景

v0.5.2 实测:MSI 安装成功后,开始菜单与桌面均无任何快捷方式,用户找不到应用入口(`C:\Program Files\Muses\Muses.exe`)。根因是 `composeApp/build.gradle.kts` 的 `nativeDistributions` 未配置任何 Windows 快捷方式选项,Compose Desktop 默认不创建入口。同时 MSI 未固定 `upgradeUuid`(UpgradeCode 随机),后续版本 MSI 无法覆盖升级旧版本。

## Requirements

1. `composeApp/build.gradle.kts` 的 `nativeDistributions` 内新增 `windows` 块:
   - `menu = true`:安装后在开始菜单创建快捷方式;
   - `menuGroup = "Muses"`:开始菜单分组文件夹名;
   - `shortcut = true`:创建桌面快捷方式;
   - `dirChooser = true`:安装时允许选择安装目录;
   - `upgradeUuid = "5d86c48d-082d-4cb1-911f-17f7fe6676c5"`:固定 UpgradeCode(一次性生成,长期不变)。
2. `.trellis/spec/ci` 发包流水线契约补充:快捷方式配置说明、upgradeUuid 变更禁忌(改了就失去覆盖升级能力)、旧版(未固定 UUID 的 v0.5.2)升级需先手动卸载的说明。

## Acceptance Criteria

- [ ] `composeApp/build.gradle.kts` 含上述 `windows` 块,Gradle 配置阶段无报错(`gradle help` 或打包任务可解析)。
- [ ] 开始菜单、桌面快捷方式与覆盖升级的结论已写入 `.trellis/spec/ci` 发包契约。
- [ ] (验证性,可选)本地执行 `packageMsi` 后,检查生成的 MSI 中包含 Shortcut 注册表项/开始菜单快捷方式(无本地打包环境则 CI 下个版本验证)。

## Notes

- 旧版 v0.5.2 及之前的 MSI 在安装本修复后的首个版本时,UpgradeCode 不匹配,不会被自动覆盖,需先卸载;自首个修复版本起,后续版本可正常覆盖升级。
