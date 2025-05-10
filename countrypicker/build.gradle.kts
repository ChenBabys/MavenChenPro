plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

apply(from = "../maven-publish-jitpack.gradle")

android {
    namespace = "com.sahooz.library.countrypicker"
    compileSdk = 34

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    viewBinding {
        enable = true
    }

//    如果某个模块需要自定义 groupId 或 version，可以在它的 build.gradle 里覆盖：
//    module/build.gradle

//    group = "com.your.custom.group" // 覆盖根配置
//    version = "2.0.0" // 覆盖根配置

}


// 配置插件参数（如果任意一个未设置，发布将被跳过）
// version如果带-SNAPSHOT就是发布到snapshot仓库，否则发布到release仓库
//publishConfig {
//    groupId = "com.godox"
//    artifactId = "countrypicker"
//    version = "1.0.8-SNAPSHOT"
//}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.recyclerview)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
