plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("maven-publish")
}

android {
    namespace = "com.app.videosdk"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // ✅ Required: rules applied automatically to consuming apps
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {

        // ✅ Release build for SDK
        release {
            isMinifyEnabled = true

            // ❌ Do NOT shrink resources in libraries
            isShrinkResources = false

            // Only default optimized rules
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt")
            )
        }

        // ✅ Debug build: no ProGuard
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

}


dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.material3.android)

    // Compose
    implementation(libs.ui)
    implementation(libs.androidx.runtime)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.material.icons.core)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.runtime.livedata)
    implementation(libs.androidx.paging.compose)
    implementation(libs.ui.tooling.preview)
    debugImplementation(libs.ui.tooling)

    // Media3 / ExoPlayer
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.exoplayer.hls)
    implementation(libs.androidx.media3.exoplayer.dash)
    implementation(libs.androidx.media3.cast)
    implementation(libs.androidx.media3.session)

    // Cast
    implementation(libs.androidx.mediarouter)
    implementation(libs.play.services.cast.framework)

    // Others
    implementation(libs.google.gson)
    implementation(libs.google.accompanist.systemuicontroller)
    implementation(libs.coil.compose)

    // Lifecycle
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)

    // Tests
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

/* ============================================================
   👇 THIS MUST BE AT THE VERY END OF THE FILE
   ============================================================ */

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])

                groupId = "com.github.kamleshmultitv"
                artifactId = "mtvplayersdk"
                version = "v1.0.11"
            }
        }
    }
}


