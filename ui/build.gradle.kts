plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

val publish_group_id = "com.chen"
val publish_version = "1.0.0"

apply(from = "../maven-publish-jitpack.gradle")

android {
    namespace = "com.godox.ui"
    compileSdk = 34

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }

}


dependencies {
    compileOnly(project(":base")) // 仅需要编译时的依赖，引用`ui`库的项目无法直接使用它
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

//publishConfig {
//    groupId = "com.godox"
//    artifactId = "common-ui"
//    version = "1.0.4-SNAPSHOT"
//}
