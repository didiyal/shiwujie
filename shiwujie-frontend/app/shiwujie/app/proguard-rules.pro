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