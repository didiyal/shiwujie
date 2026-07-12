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
            isMinifyEnabled = true
            isShrinkResources = true
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