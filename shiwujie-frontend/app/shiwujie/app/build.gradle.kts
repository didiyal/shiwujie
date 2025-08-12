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
    
    // 按照讯飞官方文档要求配置jniLibs
    sourceSets {
        getByName("main") {
            jniLibs.srcDirs("src/main/Jnilibs")
        }
    }
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
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.gson)
    
    // WebSocket
    implementation(libs.java.websocket)
    
    // AnyRTC SDK (官方Maven仓库)
    implementation("io.anyrtc:rtc:4.3.1.2")
    implementation(files("libs\\Msc.jar"))

    // Test
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}