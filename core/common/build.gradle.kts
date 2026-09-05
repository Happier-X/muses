// P1 KMP 公共模块（任务 09-04-kmp-p1-common）：android + jvm 双 target。
// AGP 9 起 KMP 模块必须用 com.android.kotlin.multiplatform.library；Android 配置收敛在
// kotlin { android { ... } } 内（AGP 把扩展挂在 Kotlin 扩展上，不再有顶层 android {}）。
// commonMain 只收严格平台无关代码；androidMain/jvmMain 暂空占位供 P2 actual 用。
// P2b-S0 spike：Room KMP 插件链验证（room 插件 × kmp.library interplay 门禁）。
plugins {
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.room)
}

kotlin {
    jvm()

    android {
        namespace = "com.muses.player.core.common"
        compileSdk = 37
        minSdk = 26
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            // P2b-S0：room-runtime + sqlite-bundled 进 commonMain（官方 Room-KMP 路径）
            implementation(libs.room.runtime)
            // sqlite-bundled 用 api：平台接线（DatabaseModule/MigrationTest 在 :core:data）同需 driver
            api(libs.sqlite.bundled)
            // P2b-S2：datastore-preferences（KMP，含 jvm 变体）+ okio（createWithPath Path）
            implementation(libs.datastore.preferences)
            implementation(libs.okio)
            // P2c：Ktor-client（CIO）传输层进 commonMain（AC4 经 ktor-bom 统一版本，
            // BOM 约束见底部 dependencies 块 `commonMainApi(platform(...))`，sourceSets 内无 platform 作用域）
            // core/api 暴露：下游 :core:lyrics/:core:scrape/:core:webdav 的包装器公有签名含 HttpClient
            api(libs.ktor.client.core)
            implementation(libs.ktor.client.cio)
            api(libs.kotlinx.io.core)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
        }
        // S1：桌面凭据 DPAPI 经 JNA（仅 jvmMain 可见，不进 commonMain；VLCJ 同理零渗入）
        jvmMain.dependencies {
            implementation(libs.jna)
            implementation(libs.jna.platform)
        }
        // S1：桌面 jvmTest（Room 文件库/内存库 + DataStore 真实路径 + DPAPI 回退 roundtrip）
        jvmTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
        // W3 写回链 KMP 化：jaudiotagger 标签实现（design.md §1「jvmMain & androidMain 同库双端」）。
        // jvmShared 中间层由 jvmMain 与 androidMain 共同 dependsOn，一份代码双端编译；
        // jaudiotagger 为纯 JVM 库（android/jvm 均可加载），implementation 不向上游传递。
        val jvmShared by creating {
            dependsOn(commonMain.get())
            dependencies {
                implementation(libs.jaudiotagger)
            }
        }
        jvmMain.get().dependsOn(jvmShared)
        androidMain.get().dependsOn(jvmShared)
    }
}

// S0：KSP 三路（commonMetadata + android + jvm）喂 room-compiler
dependencies {
    add("kspCommonMainMetadata", libs.room.compiler)
    add("kspAndroid", libs.room.compiler)
    add("kspJvm", libs.room.compiler)
    // P2c AC4：Ktor 版本经 BOM 统一（sourceSets 块内无 platform 作用域，故在此声明约束）
    add("commonMainApi", platform(libs.ktor.bom))
}

// S0：schemas 导出目录指到 core/common/schemas（R2 时旧 core/data/schemas 迁移后删除）
room {
    schemaDirectory("$projectDir/schemas")
}
