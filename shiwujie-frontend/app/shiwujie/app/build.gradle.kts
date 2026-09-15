plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.swj.shiwujie"
    compileSdk = 35
    buildToolsVersion = "36"

    defaultConfig {
        applicationId = "com.swj.shiwujie"
        minSdk = 30
        targetSdk = 35
        versionCode = 29
        versionName = "3.2.6"  // 3.2.6：我的页支持修改绑定手机号/修改用户名/设置或修改密码，去掉修改个人信息   // 3.2.5：紧急求助弹窗全 tab/后台可达（信令监听上收 Activity）；家庭成员列表补家主；"无名"兜底用户+尾号   // 3.2.4：修我的页进家庭/社区后 tabBar 切换不生效（替换式导航）；产品介绍补"直接问前方是什么"   // 3.2.3：修复点"我的"进个人中心即闪退（requireView 在视图挂载前被调用）   // 3.2.2：我的页重排（默认名用户+尾号/手机号上移/家庭社区可点击）；志愿者端 tabBar 与家庭页改造；帮助钮居首   // 3.2.1：拍照识别报错不再播英文（TTS 兜底清洗）；产品介绍充实语音能力   // 3.2.0：单页化重构——删 tabBar，AI 页左侧六圆钮（语音/拍照/紧急/志愿者/我的/帮助），家庭/社区收进我的并加返回钮；一键登录免设密码   // 3.1.16：紧急求助无家庭弹引导窗；加入/创建家庭取消审核直连生效并重启WS   // 3.1.15：WS 心跳 30s→15s + 心跳发送失败当场重连（断线感知减半）   // 3.1.14：志愿者等待窗不再自动复活（START_NOT_STICKY）；盲人连点防抖真正生效   // 3.1.13：身份/实名认证全部下线（双端不再弹窗不再展示）；志愿者端退后台不再播报   // 3.1.12：进入软件不再弹残疾证身份校验   // 3.1.11：紧急求助 60 秒无家属接通自动取消并播报；家属端收回弹窗时语音播报；接听落败播报   // 3.1.10：AI 任务进行中禁点语音/拍照；视频通话退后台不播提示   // 3.1.9：息屏保活（WakeLock）+ 拍照播报缩至 60 字   // 3.1.8：WS 未连接时禁止发起求助（防志愿者单侧干等）   // 3.1.7：挂断后通话标志复位（onResume）+ 服务端 type=5 对端通知   // 3.1.6：紧急求助状态残留自愈（退出竞态/杀进程不再卡死）   // 3.1.5：匹配失败 TTS 播报 + 服务端残留等待记录自动过期   // 3.1.4：WS 心跳安全网重连 + 重连后业务闸门复位   // 3.1.3：强更链路根修（DownloadManager URI + 缓存中转安装）   // 3.1.1：强更防旧缓存/循环（时间戳文件名+安装前版本校验）

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // 签名：项目专属 release.jks（2026-09-12 新建，测试期统一签名身份；密码随仓库提交——
    // 正式上线前如需轮换，换 keystore + 改此处即可，见 docs/android.md）
    signingConfigs {
        create("release") {
            storeFile = file("../apk/release.jks")
            storePassword = "shiwujie2026"
            keyAlias = "shiwujie"
            keyPassword = "shiwujie2026"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true   // 生成 BuildConfig（RetrofitClient / MyApplication 按 BuildConfig.DEBUG 守卫日志）
    }
    
    // JNI libs 由 Android Gradle Plugin 自动发现 src/main/jniLibs/，无需手动指定
}



dependencies {
    // AndroidX
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    
    // Network
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.retrofit.converter.scalars)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.gson)
    
    // WebSocket
    implementation(libs.java.websocket)
    
    // AnyRTC SDK (本地 AAR，避免远程下载)
    implementation(files("libs/rtc-release-4.3.1.3.aar"))
    // iFlytek MSC SDK (TTS/语音识别)
    implementation(files("libs/Msc.jar"))

    // Test
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}