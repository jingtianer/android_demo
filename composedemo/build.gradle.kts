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
}

// ======================== 版本常量管理 ========================
val configFile = project.file("app-config.properties")
val configProps = Properties()
if (configFile.exists()) {
    FileInputStream(configFile).use { configProps.load(it) }
} else {
    throw GradleException("配置文件 app-config.properties 不存在！")
}

// 应用配置
val appName = configProps.getProperty("APP_NAME")
val appVersion = configProps.getProperty("APP_VERSION")
val appAid = configProps.getProperty("AID")

// 第三方库版本
object Versions {
    const val kotlinCoroutines = "1.7.3"
    const val compose = "1.7.6"
    const val room = "2.7.1"
    const val androidCore = "1.8.0"
    const val appCompat = "1.4.1"
    const val javacv = "1.5.9"
    const val ffmpegPlatform = "6.0-1.5.9"
    const val jaudiotagger = "3.0.1"
    const val junit = "4.13.2"
    const val androidJunit = "1.3.0"
    const val espresso = "3.7.0"
    const val annotations = "24.0.1"
    const val gson = "2.8.8"
    const val activityCompose = "1.8.0"
    const val recyclerView = "1.2.0"
    const val documentFile = "1.0.0"
    const val lifecycle = "2.8.1"
    const val constraintLayoutCompose = "1.0.1"
}

// ======================== Kotlin 多平台配置 ========================
kotlin {
    jvmToolchain(17)

    androidTarget()

    jvm("desktop") {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }

    sourceSets {
        // 通用依赖 (CommonMain)
        val commonMain by getting {
            dependencies {
                // Compose 核心
                implementation(compose.runtime) {
                    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-android")
                    exclude(group = "com.intellij", module = "annotations")
                }
                implementation(compose.foundation) {
                    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-android")
                    exclude(group = "com.intellij", module = "annotations")
                }
                implementation(compose.material3) {
                    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-android")
                    exclude(group = "com.intellij", module = "annotations")
                }
                implementation(compose.ui) {
                    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-android")
                    exclude(group = "com.intellij", module = "annotations")
                }
                implementation(compose.components.resources) {
                    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-android")
                    exclude(group = "com.intellij", module = "annotations")
                }
                implementation("androidx.compose.ui:ui-tooling-preview:${Versions.compose}") {
                    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-android")
                    exclude(group = "com.intellij", module = "annotations")
                }

                // 协程核心
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.kotlinCoroutines}") {
                    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-android")
                    exclude(group = "com.intellij", module = "annotations")
                }

                // 数据库 - Room
                implementation("androidx.room:room-runtime:${Versions.room}") {
                    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-android")
                    exclude(group = "com.intellij", module = "annotations")
                }
                implementation("androidx.room:room-ktx:${Versions.room}") {
                    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-android")
                    exclude(group = "com.intellij", module = "annotations")
                }

                // 生命周期
                implementation("androidx.lifecycle:lifecycle-viewmodel:${Versions.lifecycle}") {
                    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-android")
                    exclude(group = "com.intellij", module = "annotations")
                }
                implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:${Versions.lifecycle}") {
                    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-android")
                    exclude(group = "com.intellij", module = "annotations")
                }
                implementation("org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose:${Versions.lifecycle}") {
                    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-android")
                    exclude(group = "com.intellij", module = "annotations")
                }

                // 工具类
                implementation("com.google.code.gson:gson:${Versions.gson}") {
                    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-android")
                    exclude(group = "com.intellij", module = "annotations")
                }

            }
        }

        // Android 平台依赖 (AndroidMain)
        val androidMain by getting {
            kotlin.exclude("composeResources/drawable")
            dependencies {
                // Android 核心
                implementation("androidx.core:core-ktx:${Versions.androidCore}")
                implementation("androidx.appcompat:appcompat:${Versions.appCompat}")
                implementation(compose.uiTooling)

                // Android 协程
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:${Versions.kotlinCoroutines}")

                // UI 组件
                implementation("androidx.activity:activity-compose:${Versions.activityCompose}")
                implementation("androidx.recyclerview:recyclerview:${Versions.recyclerView}")
                implementation("androidx.documentfile:documentfile:${Versions.documentFile}")
            }
        }

        // Desktop 平台依赖 (DesktopMain)
        val desktopMain by getting {
            dependencies {
                // Compose Desktop 核心
                implementation(compose.desktop.currentOs) {
                    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-android")
                }

                // 注解
                implementation("org.jetbrains:annotations:${Versions.annotations}") {
                    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-android")
                }

                // 工具类
                implementation("com.google.code.gson:gson:${Versions.gson}") {
                    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-android")
                }
                implementation("androidx.sqlite:sqlite-bundled:2.6.2") {
                    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-android")
                }

                // 数据库 - Room (仅声明运行时，编译器通过 KSP 处理)
                implementation("androidx.room:room-runtime:${Versions.room}") {
                    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-android")
                }
                implementation("androidx.room:room-ktx:${Versions.room}") {
                    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-android")
                }

                // Desktop 协程
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:${Versions.kotlinCoroutines}") {
                    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-android")
                }
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.kotlinCoroutines}") {
                    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-android")
                }

                // 多媒体处理
                implementation("org.bytedeco:javacv:${Versions.javacv}") {
                    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-android")
                }
                implementation("org.bytedeco:ffmpeg-platform:${Versions.ffmpegPlatform}") {
                    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-android")
                }
                implementation("net.jthink:jaudiotagger:${Versions.jaudiotagger}") {
                    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-android")
                }
            }
        }
    }
}

// ======================== Android 配置 ========================
android {
    namespace = appAid
    compileSdk = 34

    defaultConfig {
        applicationId = appAid
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = appVersion

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
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            resValue("string", "app_name", appName)
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
        kotlinCompilerExtensionVersion = Versions.compose
    }
}

// ======================== 全局依赖 ========================
dependencies {
    // Room 编译器 (KSP)
    ksp("androidx.room:room-compiler:${Versions.room}")

    // Room 扩展
    implementation("androidx.room:room-rxjava2:${Versions.room}")
    implementation("androidx.room:room-paging:${Versions.room}")

    // 生命周期扩展
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:${Versions.lifecycle}")
    implementation("androidx.compose.runtime:runtime-livedata:1.0.0")

    // Compose 布局
    implementation("androidx.constraintlayout:constraintlayout-compose:${Versions.constraintLayoutCompose}")

    // Compose UI 核心
    implementation("androidx.compose.ui:ui:${Versions.compose}")
    implementation("androidx.compose.ui:ui-graphics:${Versions.compose}")

    // 测试依赖
    testImplementation("junit:junit:${Versions.junit}")
    androidTestImplementation("androidx.test.ext:junit:${Versions.androidJunit}")
    androidTestImplementation("androidx.test.espresso:espresso-core:${Versions.espresso}")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:${Versions.compose}")
    debugImplementation("androidx.compose.ui:ui-tooling:${Versions.compose}")
    debugImplementation("androidx.compose.ui:ui-test-manifest:${Versions.compose}")
}

// ======================== Compose Desktop 配置 ========================
compose {
    resources {
        generateResClass = auto
    }

    desktop.application {
        mainClass = "com.jingtian.composedemo.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Exe, TargetFormat.Deb, TargetFormat.Dmg)
            packageName = appName
            packageVersion = appVersion
            description = "Compose Desktop Demo App"
            includeAllModules = true
            copyright = "© 2778 jingtian.meow. All rights reserved."
            vendor = "jingtian.meow"

            windows {
                shortcut = true
                dirChooser = true
                perUserInstall = false
            }

            linux {}

            macOS {
                bundleID = appAid
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