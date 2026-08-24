import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// 纯 Kotlin 领域模型模块：禁止任何 Android/网络/持久化依赖
plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    testImplementation(libs.junit)
}
