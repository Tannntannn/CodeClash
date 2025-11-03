plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.codeclash"
    compileSdk = 35

    androidResources {
        ignoreAssetsPattern ="!.svn:!.git:!.gitignore:!.ds_store:!*.scc:<dir>_*:!CVS:!thumbs.db:!picasa.ini:!*~"
    }

    buildFeatures{
        viewBinding = true
    }

    defaultConfig {
        applicationId = "com.example.codeclash"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Reduce APK size by only including 64-bit ABI in release builds
        ndk {
            abiFilters += listOf("arm64-v8a")
        }
    }

    buildTypes {
        release {
            // Enable code shrinking/obfuscation and resource shrinking for smaller APK
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.firebase.database)
    implementation(libs.firebase.auth)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation ("com.google.firebase:firebase-firestore:24.10.0")
    // Firebase Cloud Messaging
    implementation("com.google.firebase:firebase-messaging:24.0.0")
    implementation(platform("com.google.firebase:firebase-bom:33.15.0"))
    implementation("org.godotengine:godot:4.4.1.stable")
    // OkHttp for JDoodle API calls
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.json:json:20231013")
    // SwipeRefreshLayout dependency
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    // MPAndroidChart for data visualization
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

}