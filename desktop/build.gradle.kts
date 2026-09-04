plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    // 本机只有 JDK 21（Android Studio jbr / jdk-21.0.11+10），工具链取 21；
    // 语言/字节码仍对齐全仓 Java 17（app 模块 source/targetCompatibility=17）。
    jvmToolchain(21)
}

dependencies {
    implementation(project(":core:common"))
    // 与 :core:common 同版本线（只复用、不升级）：
    // room/datastore/JNA 在 :core:common 内为 implementation 作用域，不透传给 JVM 消费方，
    // 桌面侧直接调用 createJvmDatabase/DataStore/PlatformCryptoEngine，故在此显式声明同版本。
    implementation(libs.room.runtime)
    implementation(libs.sqlite.bundled)
    implementation(libs.datastore.preferences)
    implementation(libs.jna)
    implementation(libs.jna.platform)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(platform(libs.ktor.bom))
    implementation(libs.ktor.client.cio)
    // S2 解码：VLCJ 只进本模块，禁止进 commonMain（P3 约束；GPL 系见 spike.md §2，D1 已接受）
    implementation(libs.vlcj)

    testImplementation(kotlin("test"))
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
