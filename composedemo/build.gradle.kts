import com.codingfeline.buildkonfig.compiler.FieldSpec
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.android.application)
    alias(libs.plugins.buildkonfig.gradle.plugin)
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
val targetPlatform = configProps.getProperty("TARGET_PLATFORM")
val isRemote = configProps.getProperty("IS_REMOTE").toBoolean()


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

    if (targetPlatform == "desktop") {
        configurations {
            all {
                exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-android")
            }
        }
    }

    sourceSets {
        // 通用依赖 (CommonMain)
        val commonMain by getting {
            dependencies {
                // Compose 核心
                implementation(compose.runtime) {
                    exclude(group = "com.intellij", module = "annotations")
                }
                implementation(compose.foundation) {
                    exclude(group = "com.intellij", module = "annotations")
                }
                implementation(compose.material3) {
                    exclude(group = "com.intellij", module = "annotations")
                }
                implementation(compose.ui) {
                    exclude(group = "com.intellij", module = "annotations")
                }
                implementation(compose.components.resources) {
                    exclude(group = "com.intellij", module = "annotations")
                }
                implementation(libs.androidx.compose.ui.tooling.preview.get().toString()) {
                    exclude(group = "com.intellij", module = "annotations")
                }

                // 协程核心
                implementation(libs.kotlinx.coroutines.core.get().toString()) {
                    exclude(group = "com.intellij", module = "annotations")
                }

                // 数据库 - Room
                implementation(libs.androidx.room.runtime.get().toString()) {
                    exclude(group = "com.intellij", module = "annotations")
                }
                implementation(libs.androidx.room.ktx.get().toString()) {
                    exclude(group = "com.intellij", module = "annotations")
                }

                // 生命周期
                implementation(libs.androidx.lifecycle.viewmodel.core.get().toString()) {
                    exclude(group = "com.intellij", module = "annotations")
                }
                implementation(libs.androidx.lifecycle.viewmodel.ktx.get().toString()) {
                    exclude(group = "com.intellij", module = "annotations")
                }
                implementation(libs.androidx.lifecycle.viewmodel.compose.get().toString()) {
                    exclude(group = "com.intellij", module = "annotations")
                }

                // 工具类
                implementation(libs.gson.get().toString()) {
                    exclude(group = "com.intellij", module = "annotations")
                }

            }
        }

        // Android 平台依赖 (AndroidMain)
        val androidMain by getting {
            kotlin.exclude("composeResources/drawable")
            dependencies {
                // Android 核心
                implementation(libs.androidx.core.ktx)
                implementation(libs.androidx.appcompat)
                implementation(compose.uiTooling)

                // Android 协程
                implementation(libs.kotlinx.coroutines.android)

                // UI 组件
                implementation(libs.androidx.activity.compose)
                implementation(libs.androidx.recyclerview)
                implementation(libs.androidx.documentfile)
                implementation(libs.jsch)
                implementation(libs.jzlib)
            }
        }

        // Desktop 平台依赖 (DesktopMain)
        val desktopMain by getting {
            dependencies {
                // Compose Desktop 核心
                implementation(compose.desktop.currentOs)

                // 注解
                implementation(libs.annotations.get().toString())

                // 工具类
                implementation(libs.gson.get().toString())
                implementation(libs.androidx.sqlite.bundled.get().toString())

                // 数据库 - Room (仅声明运行时，编译器通过 KSP 处理)
                implementation(libs.androidx.room.runtime.get().toString())
                implementation(libs.androidx.room.ktx.get().toString())

                // Desktop 协程
                implementation(libs.kotlinx.coroutines.swing.get().toString())
                implementation(libs.kotlinx.coroutines.core.get().toString())

                // 多媒体处理
                implementation(libs.javacv.get().toString())
                implementation(libs.ffmpeg.platform.get().toString())
                implementation(libs.jaudiotagger.get().toString())
            }
        }
    }
}

buildkonfig {
    packageName = appAid

    defaultConfigs {
        buildConfigField(FieldSpec.Type.BOOLEAN, "isRemote", "$isRemote")
    }
    targetConfigs {
        // names in create should be the same as target names you specified
        create("android") {
            buildConfigField(FieldSpec.Type.BOOLEAN, "isRemote", "$isRemote")
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
        val remoteSuffix = if (isRemote) {
            ".remote"
        } else {
            ""
        }
        debug {
            applicationIdSuffix = "$remoteSuffix.debug"
            resValue("string", "app_name", "${appName}$remoteSuffix.debug")
        }
        release {
            if (remoteSuffix.isNotBlank()) {
                applicationIdSuffix = "$remoteSuffix"
            }

            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            resValue("string", "app_name", "${appName}$remoteSuffix")
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
        kotlinCompilerExtensionVersion = libs.versions.compose.ui.get()
    }
}

// ======================== 全局依赖 ========================
dependencies {
    // Room 编译器 (KSP)
    ksp(libs.androidx.room.compiler)

    // Room 扩展
    implementation(libs.androidx.room.rxjava2)
    implementation(libs.androidx.room.paging)

    // 生命周期扩展
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.compose.runtime.livedata)

    // Compose 布局
    implementation(libs.androidx.constraintlayout.compose)

    // Compose UI 核心
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)

    // 测试依赖
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
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
