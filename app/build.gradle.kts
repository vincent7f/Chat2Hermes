import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.herdroid.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.herdroid.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.markdownRendererAndroid)
    implementation(libs.markdownRendererM3)
    testImplementation(libs.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

/** Copies `app-debug.apk` to `Herdroid-debug-<yyyyMMdd-HHmmss>.apk` under debug outputs and to Baidu Sync. */
val archiveHerdroidDebugApk =
    tasks.register("archiveHerdroidDebugApk") {
        group = "build"
        description =
            "Copy debug APK as Herdroid-debug-<timestamp>.apk to outputs/apk/debug and D:/BaiduSyncdisk/apk/Herdroid"
        dependsOn("assembleDebug")
        doLast {
            val ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
            val fileName = "Herdroid-debug-$ts.apk"
            val source = layout.buildDirectory.get().asFile.resolve("outputs/apk/debug/app-debug.apk")
            check(source.exists()) {
                "Missing debug APK: ${source.absolutePath}. Run assembleDebug first."
            }
            val outDir = layout.buildDirectory.get().asFile.resolve("outputs/apk/debug")
            outDir.mkdirs()
            val destProject = outDir.resolve(fileName)
            source.copyTo(destProject, overwrite = true)
            val syncRoot = rootProject.file("D:/BaiduSyncdisk/apk/Herdroid")
            syncRoot.mkdirs()
            val destSync = syncRoot.resolve(fileName)
            source.copyTo(destSync, overwrite = true)
            logger.lifecycle("Herdroid debug archive: ${destProject.absolutePath}")
            logger.lifecycle("Herdroid sync copy:     ${destSync.absolutePath}")
        }
    }

afterEvaluate {
    tasks.named("assembleDebug").configure {
        finalizedBy(archiveHerdroidDebugApk)
    }
}
