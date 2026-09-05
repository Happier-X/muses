# 桌面端正式应用图标（MSI/托盘）

## Goal

沿用安卓占位图标设计语言(深色圆底 #14141A + 浅蓝播放三角 #7FB5FF,见 `app/src/main/res/drawable/ic_launcher_foreground.xml`)程序化生成多尺寸 PNG 并拼 ICO,经 `windows.iconFile` 配置 jpackage 图标替换默认咖啡杯;DesktopTray 改为优先读 classpath 图标资源,失败回落程序化绘制。

## 背景

- 安卓端图标本身即占位(自适应图标:深色底 + 播放三角),桌面端 Windows 安装包目前是 jpackage 默认咖啡杯图标,托盘为上一任务的程序化圆底 M 占位。
- Compose DSL 1.12.0-rc01 的 `AbstractPlatformSettings.iconFile: RegularFileProperty`(反编译确认)即 Windows 图标入口,接受 .ico。
- 项目无设计稿,本任务以程序化生成方式统一两端占位设计语言;正式设计稿图标两端替换时再起任务。

## Requirements

1. 图标资源(一次性生成脚本产物,入库):
   - ICO(jpackage 用,含 16/24/32/48/64/128/256 PNG 条目)放 `composeApp/icons/muses.ico`;
   - 托盘 PNG(32px)放 `desktop/src/main/resources/muses/tray-icon.png`;
   - 视觉:圆形深底 #14141A + 浅蓝 #7FB5FF 播放三角,三角宽度约画布 40%(Windows 无 launcher mask,比安卓 30% 适当放大),光学居中略右移。
2. `composeApp/build.gradle.kts`:`windows { iconFile = file("icons/muses.ico") }`。
3. `DesktopTray.createIconImage()`:优先读 classpath `/muses/tray-icon.png`,读取失败回落现有程序化绘制(不抛异常)。
4. 生成脚本不保留(一次性);图标文件入库供复现与 CI 使用。

## Acceptance Criteria

- [ ] ICO 经工具读取验证尺寸条目正确;`packageMsi` 本地打包成功(jpackage 接受 ico)。
- [ ] 运行实测托盘显示新图标(截图确认)。
- [ ] 改动范围:新增图标文件 + `build.gradle.kts` 一行 + `DesktopTray` 图标加载逻辑,不触碰 scrape 相关模块。

## Notes

- 安卓端 launcher 图标替换与 Windows 同步换正式设计稿,不在本任务范围。
