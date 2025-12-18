plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.app.sample"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.app.sample"
        minSdk = 24
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
            buildConfigField(
                "String",
                "BASE_URL",
                "\"https://api.artofliving.app/artoflivingapi/v10/\""
            )
            buildConfigField(
                "String",
                "MASTER_JSON_URL",
                "\"https://static.artofliving.app/configration/717/master_prod.json\""
            )
            buildConfigField(
                "String",
                "DRM_LICENSE_URL",
                "\"https://widevine-dash.ezdrm.com/widevine-php/widevine-foreignkey.php?pX=63CF74&\""
            )
            buildConfigField(
                "String",
                "STATIC_AUTH_TOKEN",
                "\"abd07061a3dd9851e3c9dd551e68e26838b29e87b2baa479c0eb53c95cac2e6bd701b5588ca7a85de55c6504e0c84c44edc468ae6fdb7a48cf170ee055cd7b3a5960795cf0c3d2989f1aedec0d93fd9d\""
            )
        }
        debug {
            buildConfigField(
                "String",
                "BASE_URL",
                "\"https://api.artofliving.app/artoflivingapi/v10/\""
            )
            buildConfigField(
                "String",
                "MASTER_JSON_URL",
                "\"https://static.artofliving.app/configration/717/master_prod.json\""
            )
            buildConfigField(
                "String",
                "DRM_LICENSE_URL",
                "\"https://widevine-dash.ezdrm.com/widevine-php/widevine-foreignkey.php?pX=63CF74&\""
            )
            buildConfigField(
                "String",
                "STATIC_AUTH_TOKEN",
                "\"abd07061a3dd9851e3c9dd551e68e26838b29e87b2baa479c0eb53c95cac2e6bd701b5588ca7a85de55c6504e0c84c44edc468ae6fdb7a48cf170ee055cd7b3a5960795cf0c3d2989f1aedec0d93fd9d\""
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
    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
}


dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    //Retrofit ------------------------------------------------------------------
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.adapter.rxjava2)
    //Okhttp-------------------------
    implementation(libs.logging.interceptor)
    // paging
    implementation(libs.androidx.paging.compose)
    implementation(libs.androidx.paging)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.ui)
    implementation(libs.androidx.runtime)
   //  implementation ("com.github.kamleshmultitv:mtvplayersdk:v1.0.6")
    implementation(project(":videosdk"))

}