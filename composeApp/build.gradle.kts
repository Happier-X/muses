import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
}

// U15：运行时版本号——CI 以 -Pmuses.desktop.version 注入（与下方 jpackage packageVersion
// 同源同值），构建期写入资源文件，桌面设置页「检查更新」经 classpath 读取；
// 本地/无属性时取最近 tag 本体（去 v 前缀、切掉 -22-gfa9f1883 这类后缀，
// 如 v0.5.3-22-gfa9f1883 → 0.5.3，只显示干净版本号）；
// 非 git 环境再回落 1.0.0（与 packageVersion 回落一致）。
fun resolveDesktopVersion(): String {
    (project.findProperty("muses.desktop.version") as String?)?.let { return it }
    return runCatching {
        // 配置期直接起进程取 tag（providers.exec 的 result 不允许配置期 get，
        // 会直接抛异常走回落；ProcessBuilder 最稳）
        val process = ProcessBuilder("git", "describe", "--tags", "--abbrev=0")
            .directory(project.rootDir)
            .redirectErrorStream(true)
            .start()
        check(process.waitFor(15, TimeUnit.SECONDS)) { "git describe 超时" }
        check(process.exitValue() == 0) { "git describe 非零退出" }
        process.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            .trim().removePrefix("v").takeIf { it.isNotEmpty() }
    }.getOrNull() ?: run {
        logger.warn("[muses] git describe 取版本失败，桌面版本号回落 1.0.0")
        "1.0.0"
    }
}
val musesDesktopVersion = resolveDesktopVersion()
val desktopVersionDir = layout.buildDirectory.dir("generated/desktopVersion")
val generateDesktopVersion by tasks.registering {
    outputs.file(desktopVersionDir.map { it.file("muses-desktop-version.txt") })
    // 版本属性必须声明为任务输入：否则 -P 变化时任务 up-to-date 跳过，资源不重写
    inputs.property("musesDesktopVersion", musesDesktopVersion)
    doLast {
        val out = desktopVersionDir.get().asFile.resolve("muses-desktop-version.txt")
        out.parentFile.mkdirs()
        out.writeText(musesDesktopVersion)
    }
}

kotlin {
    jvm()

    sourceSets {
        // 版本资源目录（生成任务见文件头；processResources 依赖在文件尾挂接）
        jvmMain {
            resources.srcDir(desktopVersionDir)
        }

        commonMain.dependencies {
            implementation(compose.material3)
            implementation(compose.foundation)
            implementation(compose.ui)
            implementation(libs.kotlinx.coroutines.core)
            api(project(":desktop"))
            // :desktop 用 implementation 不透传 :core:common，composeApp 需直接依赖
            api(project(":core:common"))
            implementation(project(":core:ui-shared"))
            implementation(libs.miuix.ui)
            // U11：desktopAppModules 引用 webdavCoreModule（feature:sources 对 webdav 为 implementation 不透传）
            implementation(project(":core:webdav"))
            // U2 桌面真模糊：壳层创建 HazeState 并对内容标记 hazeSource（消费层在 ui-shared）
            implementation(libs.haze)
            // U9 曲库共用化：桌面直接复用 :feature:library commonMain 的 Screen/ViewModel
            implementation(project(":feature:library"))
            // U11 音源共用化：桌面复用共享 WebDAV 浏览页（:feature:sources commonMain）
            implementation(project(":feature:sources"))
            // U12 播放端口统一：desktopAppModules 装载 playerModule（共享 PlayerViewModel/端口绑定）
            implementation(project(":feature:player"))
            // U14 刮削共用化：桌面复用共享 ScrapeScreen（手搓装配层删除）
            implementation(project(":feature:scrape"))
            // U23 桌面切共享壳：MusesApp（CMP Navigation 导航壳）+ 歌单 VM 装配
            implementation(project(":feature:shell"))
            implementation(project(":feature:playlist"))
            // 共享 ViewModel 经 Koin 注入（koinViewModel() 在 compose-viewmodel，KMP 工件；
            // KMP sourceSets 不支持 platform(BOM)，toml 已显式挂 4.2.0）
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            // Room/SQLite/DataStore/JNA 不透传，桌面直接调用 DAO 时需显式声明
            implementation(libs.room.runtime)
            implementation(libs.sqlite.bundled)
            implementation(libs.datastore.preferences)
            implementation(libs.jna)
            implementation(libs.jna.platform)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            // Dispatchers.Main 在桌面 JVM 靠 swing dispatcher 提供（CMP 1.12 起不传递，见 toml 同条目注释）
            implementation(libs.kotlinx.coroutines.swing)
            // W4 桌面装配（任务 09-05-scrape-kmp）：JaudiotaggerTagPort 在 :core:common jvmShared，
            // 其 jaudiotagger 依赖为 implementation 作用域不透传，桌面消费 TagPort 需显式声明（同版本线）
            implementation(libs.jaudiotagger)
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.muses.player.desktop.MainKt"
        // S4 打包：jpackage 需完整 JDK（含 jpackage.exe），Android Studio jbr 不带；
        // 本机 jdk-21.0.11+10 即打包用 JDK（与 :desktop jvmToolchain(21) 同版本线）。
        javaHome = System.getenv("MUSES_DESKTOP_JDK") ?: "C:/Users/zhf52/java/jdk-21.0.11+10"

        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Exe)
            packageName = "Muses"
            // CI 发版经 -Pmuses.desktop.version 从 tag 注入，本地不传时回落 1.0.0
            packageVersion = (project.findProperty("muses.desktop.version") as String?) ?: "1.0.0"
            description = "Muses Music Player"
            vendor = "Muses"

            // v0.5.2 实测：不配置则 MSI 装完无任何入口，用户找不到应用。
            // upgradeUuid 固定 UpgradeCode，缺省时每次构建随机，后续版本无法覆盖升级。
            windows {
                menu = true
                menuGroup = "Muses"
                shortcut = true
                dirChooser = true
                upgradeUuid = "5d86c48d-082d-4cb1-911f-17f7fe6676c5"
                // 与安卓占位同设计语言（深色圆底 + 浅蓝播放三角），经 jpackage --icon 注入
                iconFile = file("icons/muses.ico")
            }
        }
    }
}

// KMP jvm 资源任务挂生成依赖（资源目录经 srcDir 已声明，此处补任务级依赖）
// 修复 U23：任务实名 jvmProcessResources（原 processJvmMainResources 永不匹配，
// 隐式依赖在 Gradle 9.6.1 校验下直接 FAIL）
tasks.matching { it.name == "jvmProcessResources" }.configureEach {
    dependsOn(generateDesktopVersion)
}
