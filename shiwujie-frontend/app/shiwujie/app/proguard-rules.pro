# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# AnyRTC SDK 防混淆规则
-keep class org.ar.**{*;}
-keep class org.anyrtc.**{*;}
-keep class org.webrtc.**{*;}

# WebSocket 防混淆规则
-keep class org.java_websocket.**{*;}

# 保持网络相关类
-keep class com.swj.shiwujie.common.network.**{*;}
-keep class com.swj.shiwujie.data.model.**{*;}

# 讯飞语音听写SDK防混淆规则（按照官方文档要求）
-keep class com.iflytek.**{*;}
-keep class com.iflytek.cloud.**{*;}
-keep class com.iflytek.speech.**{*;}

# ===== 以下为 release 混淆补充（R8）=====

# 保留泛型签名（Gson/TypeToken/Retrofit 必需）、注解、源文件与行号（便于线上堆栈定位）
-keepattributes Signature, *Annotation*, SourceFile,LineNumberTable

# Gson：保留 @SerializedName 字段；保留 TypeToken 及其子类（AiFragment 用匿名 TypeToken 反序列化历史）
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

# AiFragment 内被 Gson 序列化的对话数据类（不在 data.model 包，需显式保留字段名）
-keep class com.swj.shiwujie.blind.ui.ai.AiFragment$Message { *; }
-keep class com.swj.shiwujie.blind.ui.ai.AiFragment$Conversation { *; }

# Retrofit / OkHttp（其 consumer rules 通常已含，此处显式兜底，避免 release 调用崩）
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }

# slf4j：java-websocket 传递依赖拉进 slf4j-api，但 App 未打包 binding 实现。
# 运行期 slf4j 无 binding 时退化为 NOP（java-websocket 仍正常工作），故抑制缺失类告警。
-dontwarn org.slf4j.**
-dontwarn org.slf4j.impl.StaticLoggerBinder