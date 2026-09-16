# Muses release 混淆规则（minify + shrinkResources 已启用）
# AGP 默认规则已覆盖 Activity/Service/Compose，此处只补第三方反射项

# Room：Entity/Dao 经注解处理生成实现，保持构造器与字段
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-dontwarn androidx.room.**
-keepclassmembers class * {
    @androidx.room.* <fields>;
    @androidx.room.* <methods>;
}

# Koin：模块经反射创建 ViewModel，保持 public 构造器
-keep class * extends androidx.lifecycle.ViewModel { public <init>(...); }
-dontwarn org.koin.**

# kotlinx-serialization：@Serializable 经插件生成 Serializer，保持伴生
-keepattributes *Annotation*, InnerClasses, Signature, EnclosingMethod
-keep class * implements kotlinx.serialization.KSerializer
-keepclassmembers class **$$serializer { *; }
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}

# Media3/OkHttp/Coil：走 OkHttp/Coil 官方传递规则，勿额外混淆平台网络
-dontwarn com.squareup.okhttp3.**
-dontwarn coil3.**
-dontwarn androidx.media3.**

# jaudiotagger/JNA：桌面与安卓共用，避免 stripped 本地方法名
-keep class net.jthink.** { *; }
-keep class com.sun.jna.** { *; }

# jaudiotagger 桌面分支引用（StandardArtwork.getImage 经 java.awt 解码；
# 安卓走 TagOptionSingleton.setAndroid(true) 分支不调用，R8 仅屏蔽警告）
-dontwarn java.awt.image.BufferedImage
-dontwarn javax.imageio.ImageIO
-dontwarn javax.imageio.stream.ImageInputStream
