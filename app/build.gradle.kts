import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

android {
    namespace = "com.purehub.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.purehub.app"
        minSdk = 26
        targetSdk = 36
        versionCode = providers.environmentVariable("PUREHUB_VERSION_CODE").orNull?.toIntOrNull() ?: 20
        versionName = providers.environmentVariable("PUREHUB_VERSION_NAME").orNull ?: "1.0.0-beta.19"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testInstrumentationRunnerArguments["clearPackageData"] = "true"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    val signingProperties = Properties().apply {
        val signingFile = rootProject.file("signing.properties")
        if (signingFile.exists()) signingFile.inputStream().use(::load)
    }
    val releaseStoreFile = providers.environmentVariable("PUREHUB_KEYSTORE_PATH").orNull
        ?: signingProperties.getProperty("storeFile")
    val releaseStorePassword = providers.environmentVariable("PUREHUB_KEYSTORE_PASSWORD").orNull
        ?: signingProperties.getProperty("storePassword")
    val releaseKeyAlias = providers.environmentVariable("PUREHUB_KEY_ALIAS").orNull
        ?: signingProperties.getProperty("keyAlias")
    val releaseKeyPassword = providers.environmentVariable("PUREHUB_KEY_PASSWORD").orNull
        ?: signingProperties.getProperty("keyPassword")

    signingConfigs {
        if (releaseStoreFile != null && releaseStorePassword != null && releaseKeyAlias != null && releaseKeyPassword != null) {
            create("release") {
                storeFile = rootProject.file(releaseStoreFile)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
                enableV4Signing = true
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.findByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    flavorDimensions += "distribution"
    productFlavors {
        create("standard") {
            dimension = "distribution"
        }
        create("fdroid") {
            dimension = "distribution"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    splits {
        abi {
            val bundleRequested = gradle.startParameter.taskNames.any { it.contains("bundle", ignoreCase = true) }
            isEnable = !bundleRequested
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
            isUniversalApk = true
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.09.01")
    val cameraxVersion = "1.5.0"
    val roomVersion = "2.7.2"
    val workVersion = "2.10.4"

    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.3")
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.navigation:navigation-compose:2.9.8")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.datastore:datastore-preferences:1.1.7")
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")
    implementation("androidx.security:security-crypto:1.1.0")
    implementation("androidx.work:work-runtime-ktx:$workVersion")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")
    implementation("androidx.camera:camera-video:$cameraxVersion")
    "standardImplementation"("com.google.mlkit:text-recognition:16.0.1")
    "standardImplementation"("com.google.mlkit:text-recognition-chinese:16.0.1")
    "fdroidImplementation"("cz.adaptech.tesseract4android:tesseract4android:4.9.0")
    "fdroidImplementation"("com.google.code.findbugs:jsr305:3.0.2")
    implementation("com.google.zxing:core:3.5.4")

    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("com.google.android.material:material:1.12.0")
    implementation("io.insert-koin:koin-android:4.0.4")
    implementation("io.insert-koin:koin-androidx-compose:4.0.4")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20231013")
    testImplementation("org.robolectric:robolectric:4.14.1")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
