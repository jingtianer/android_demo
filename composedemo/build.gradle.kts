import org.gradle.internal.fingerprint.classpath.impl.ClasspathFingerprintingStrategy.runtimeClasspath
import org.gradle.internal.impldep.org.junit.experimental.categories.Categories
import org.gradle.internal.impldep.org.junit.experimental.categories.Categories.CategoryFilter.exclude
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import java.io.FileInputStream
import java.util.Properties

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    kotlin("plugin.compose")
    id("com.google.devtools.ksp")
    id("org.jetbrains.compose")
    id("com.android.application")
//    id("org.jetbrains.kotlin.android")
//    id("com.chaquo.python")
}

val configFile = project.file("app-config.properties")
val configProps = Properties()
if (configFile.exists()) {
    FileInputStream(configFile).use {
        configProps.load(it)
    }
} else {
    throw GradleException("配置文件 config.properties 不存在！")
}

val appName = configProps.getProperty("APP_NAME")
val version = configProps.getProperty("APP_VERSION")
val aid = configProps.getProperty("AID")
val room_version = "2.7.1"
val composeVersion = "1.7.6"

kotlin {
    jvmToolchain(17)

    androidTarget {
//        compilations.all {
//            kotlinOptions {
//                jvmTarget = "17"
//            }
//        }
    }

    jvm("desktop") {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }

    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime) {
                    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-android")
                }
                implementation(compose.foundation) {
                    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-android")
                }
                implementation(compose.material3) {
                    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-android")
                }
                implementation(compose.ui) {
                    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-android")
                }
                implementation(compose.components.resources) {
                    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-android")
                }
                implementation("com.google.code.gson:gson:2.8.8")
                implementation("androidx.compose.ui:ui-tooling-preview:$composeVersion") {
                    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-android")
                }
                implementation("androidx.lifecycle:lifecycle-viewmodel:2.8.1") {
                    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-android")
                }
                implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.1") {
                    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-android")
                }
                implementation("org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose:2.8.1") {
                    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-android")
                }
                implementation("androidx.sqlite:sqlite-bundled:2.6.2") {
                    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-android")
                }
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3") {
                    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-android")
                }
                implementation("androidx.room:room-runtime:$room_version") {
                    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-android")
                    exclude(group = "com.intellij", module = "annotations")
                }
                implementation("androidx.room:room-compiler:$room_version") {
                    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-android")
                    exclude(group = "com.intellij", module = "annotations")
                }
                implementation("androidx.room:room-ktx:$room_version") {
                    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-android")
                    exclude(group = "com.intellij", module = "annotations")
                }
            }
        }

        val androidMain by getting {
            kotlin.exclude("composeResources/drawable")
            dependencies {
                implementation("androidx.core:core-ktx:1.8.0")
                implementation("androidx.appcompat:appcompat:1.4.1")
                implementation(compose.uiTooling)
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
            }
        }

        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)

                implementation("org.jetbrains:annotations:24.0.1") {
                    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-android")
                }
                implementation("com.google.code.gson:gson:2.8.8") {
                    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-android")
                }
                implementation("androidx.room:room-runtime:$room_version") {
                    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-android")
                }
                implementation("androidx.room:room-compiler:$room_version") {
                    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-android")
                }
                implementation("androidx.room:room-ktx:$room_version") {
                    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-android")
                }
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.7.3") {
                    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-android")
                }
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3") {
                    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-android")
                }
                // 处理视频帧
                implementation("org.bytedeco:javacv:1.5.9")
                implementation("org.bytedeco:ffmpeg-platform:6.0-1.5.9")

                // 处理音频封面
                implementation("net.jthink:jaudiotagger:3.0.1")
            }
        }
    }
}

android {
    namespace = aid
    compileSdk = 34

    defaultConfig {
        applicationId = aid
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = version

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            resValue("string", "app_name", "${appName}.debug")
        }
        release {
//            isMinifyEnabled = true
//            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            resValue("string", "app_name", "$appName")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        viewBinding = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.7.6"
    }
//    packagingOptions {
//        resources {
//            excludes += "/META-INF/{AL2.0,LGPL2.1}"
//        }
//    }
}
//执行python 接入文档: https://chaquo.com/chaquopy/doc/15.0/android.html

dependencies {
//    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
    // activity-compose 版本需适配 Compose 和 AGP，1.4.0 是 AGP 7.0.4 兼容的稳定版
    implementation("androidx.activity:activity-compose:1.8.0")
    implementation("androidx.recyclerview:recyclerview:1.2.0")
    implementation("androidx.documentfile:documentfile:1.0.0")

    implementation("androidx.compose.ui:ui:$composeVersion")
    implementation("androidx.compose.ui:ui-graphics:$composeVersion")

    // 测试依赖（版本适配）
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:$composeVersion")
    debugImplementation("androidx.compose.ui:ui-tooling:$composeVersion")
    debugImplementation("androidx.compose.ui:ui-test-manifest:$composeVersion")

    ksp("androidx.room:room-compiler:$room_version")
    implementation("androidx.room:room-ktx:$room_version")
    implementation("androidx.room:room-rxjava2:$room_version")
    implementation("androidx.room:room-paging:$room_version")


    implementation("androidx.lifecycle:lifecycle-viewmodel:2.8.1")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.1")
    implementation("androidx.compose.runtime:runtime-livedata:1.0.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.1")

    implementation("androidx.constraintlayout:constraintlayout-compose:1.0.1")
}

compose {
    resources {
        generateResClass = auto
    }
    desktop.application {
        mainClass = "com.jingtian.composedemo.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Exe, TargetFormat.Deb, TargetFormat.Dmg)
            packageName = appName
            packageVersion = version
            description = "Compose Desktop Demo App"
            includeAllModules = true
            copyright = "© 2778 jingtian.meow. All rights reserved."
            vendor = "jingtian.meow"

            windows {
                shortcut = true
                dirChooser = true
//                iconFile.set(project.file("src/commonMain/composeResources/drawable/icon.ico"))
                perUserInstall = false
            }
            linux {
//                iconFile.set(project.file("src/commonMain/composeResources/drawable/icon.png"))
            }
            macOS {
                bundleID = aid
//                iconFile.set(project.file("src/commonMain/composeResources/drawable/icon.icns"))
            }
        }

        buildTypes {
            release {
                proguard {
                    isEnabled = false
                    obfuscate.set(true)
                    optimize.set(false)
                    configurationFiles.from(project.file("proguard-desktop.pro"))
                }
            }
        }
    }
}