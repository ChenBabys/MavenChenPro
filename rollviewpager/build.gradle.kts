plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

apply(from = "../maven-publish-jitpack.gradle")

android {
    namespace = "com.jude.rollviewpager"
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

}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.recyclerview)
    implementation(libs.material)
}




//publishConfig {
//    groupId = "com.jude"
//    artifactId = "rollviewpager"
//    version = "1.0.0"
//}

//2.0版本可以覆盖写入
//afterEvaluate {
//    if (project.plugins.hasPlugin("maven-publish")) {
//        publishing.publications.Release.artifactId = "custom-name" // 覆盖artifactId
//        version = '2.0.0' // 覆盖版本号
//    }
//}