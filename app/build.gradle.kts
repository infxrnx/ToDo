plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.guessgame"
    compileSdk = 34
    android.ndkVersion = "28.0.12674087"
    externalNativeBuild {
        cmake {
            path = file("/src/main/cpp/CMakeLists.txt") // Путь к CMakeLists.txt
        }
    }

    defaultConfig {
        applicationId = "com.example.guessgame"
        minSdk = 24
        targetSdk = 34
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
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation("androidx.work:work-runtime-ktx:2.8.0")
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation("com.squareup.okhttp3:okhttp:4.9.1")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.squareup.picasso:picasso:2.71828")
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.swiperefreshlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

}