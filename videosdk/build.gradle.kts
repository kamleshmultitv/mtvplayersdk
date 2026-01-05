import org.gradle.api.publish.maven.MavenPublication

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
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt")
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }

    // ✅ Java target (must match Kotlin)
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17

        isCoreLibraryDesugaringEnabled = true
    }

    // ✅ Kotlin 2.x compiler options
    kotlin {
        compilerOptions {
            jvmTarget.set(
                org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
            )
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    // ✅ REQUIRED for AGP 8+ + JitPack
    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

/**
 * ✅ Maven Publish (KEEP OUTSIDE android {})
 * This publishes ONLY videosdk (not app)
 */
afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])

                groupId = "com.github.kamleshmultitv"
                artifactId = "mtvplayersdk"
                version = "mobile-1.0.46"   // 🔴 CHANGE VERSION HERE
            }
        }
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    // Core
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

    implementation("androidx.media3:media3-exoplayer-ima:1.3.1")
    implementation("com.google.ads.interactivemedia.v3:interactivemedia:3.34.0")
}
