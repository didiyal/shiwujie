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
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    buildFeatures {
        viewBinding = true
    }
    
    // JNI libs 由 Android Gradle Plugin 自动发现 src/main/jniLibs/，无需手动指定
}



dependencies {
    // AndroidX
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
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

    // ===== 障碍物检测功能依赖 - 严格按照backend_service.py的摄像头功能需求 =====
    // 改造说明：将Python后端的OpenCV摄像头功能转换为Android CameraX实现
    
    // CameraX - 对应原Python代码的cv2摄像头功能
    implementation("androidx.camera:camera-core:1.3.1")
    implementation("androidx.camera:camera-camera2:1.3.1")
    implementation("androidx.camera:camera-lifecycle:1.3.1")
    implementation("androidx.camera:camera-view:1.3.1")
    
    // 图像处理 - 对应原Python代码的numpy和cv2图像处理功能
    implementation("androidx.camera:camera-extensions:1.3.1")

    // Test
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}