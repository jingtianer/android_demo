import com.codingfeline.buildkonfig.compiler.FieldSpec
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
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
	configFile.inputStream().use { configProps.load(it) }
} else {
	throw GradleException("配置文件 app-config.properties 不存在！")
}

// 应用配置
val appName = configProps.getProperty("APP_NAME")
val appVersion = configProps.getProperty("APP_VERSION")
val appAid = configProps.getProperty("AID")
val targetPlatform = configProps.getProperty("TARGET_PLATFORM")
val isRemote = configProps.getProperty("IS_REMOTE").toBoolean()

val isDesktop = targetPlatform.lowercase() == "desktop"
val isAndroid = targetPlatform.lowercase() == "android"
val isIOS = targetPlatform.lowercase() == "ios"


// ======================== Kotlin 多平台配置 ========================
kotlin {
    jvmToolchain(17)

    androidTarget()

    if (isIOS) {
        listOf(
            iosX64(),
            iosArm64(),
            iosSimulatorArm64()
        ).forEach { iosTarget->
            iosTarget.binaries {
                framework {
                    baseName = "SharedComposeDemo"
                    isStatic = true
                }
            }
        }
    }

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
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation(compose.components.resources)

                // 协程核心
                implementation(libs.kotlinx.coroutines.core.get().toString())

                // 数据库 - Room
                implementation(libs.androidx.room.runtime.get().toString())
                implementation(libs.androidx.room.ktx.get().toString())

                // 生命周期
                implementation(libs.androidx.lifecycle.viewmodel.core.get().toString())
                implementation(libs.androidx.lifecycle.viewmodel.ktx.get().toString())
                implementation(libs.androidx.lifecycle.viewmodel.compose.get().toString())

                // 工具类
                implementation(libs.kotlinx.serialization.json)
                implementation("org.jetbrains.kotlinx:kotlinx-io-core:0.6.0")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.2")

//                // Room 扩展
//                implementation(libs.androidx.room.rxjava2)
//                implementation(libs.androidx.room.paging)
//
//                // 生命周期扩展
//                implementation(libs.androidx.lifecycle.livedata.ktx)
//                implementation(libs.androidx.compose.runtime.livedata)
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
                implementation(libs.androidx.compose.ui.tooling.preview.get().toString())

                // Android 协程
                implementation(libs.kotlinx.coroutines.android)

                // UI 组件
                implementation(libs.androidx.activity.compose)
                implementation(libs.androidx.recyclerview)
                implementation(libs.androidx.documentfile)
                implementation(libs.jsch)
                implementation(libs.jzlib)

                implementation(libs.androidx.biometric)
            }
        }

        if (isIOS) {
            val iosMain by creating {
                dependsOn(commonMain)
                dependencies {
                    implementation(libs.kotlinx.coroutines.core)
                    implementation(libs.sqlite.bundled)
//                implementation(libs.androidx.room.runtime)
//                implementation(libs.androidx.room.ktx)
                }
            }

            val iosArm64Main by getting {
                dependsOn(iosMain)
            }

            val iosSimulatorArm64Main by getting {
                dependsOn(iosMain)
            }

            val iosX64Main by getting {
                dependsOn(iosMain)
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

    if (!isAndroid) {
        configurations {
            all {
                exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-android")
                exclude(group = "com.intellij", module = "annotations")
            }
        }
    }
}

buildkonfig {
    packageName = appAid

    defaultConfigs {
        buildConfigField(FieldSpec.Type.BOOLEAN, "isRemote", "$isRemote")
        buildConfigField(FieldSpec.Type.BOOLEAN, "isDesktop", "$isDesktop")
        buildConfigField(FieldSpec.Type.BOOLEAN, "isAndroid", "$isAndroid")
        buildConfigField(FieldSpec.Type.BOOLEAN, "isIOS", "$isIOS")
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
    // 仅在 JVM / Android 相关 target 上运行，避免在 iOS KSP 任务中解析依赖
    add("kspAndroid", libs.androidx.room.compiler)
    add("kspDesktop", libs.androidx.room.compiler)

    if (isIOS) {
        // iOS 仅使用 Kotlin Inject 的 KSP，不运行 Room KSP，以避免 commonMain 中 JVM-only 类型（如 java.util.Date）导致 MissingType
        add("kspIosSimulatorArm64", libs.kotlin.inject.compiler)
        add("kspIosArm64", libs.kotlin.inject.compiler)
        add("kspIosX64", libs.kotlin.inject.compiler)
        add("kspIosSimulatorArm64", libs.androidx.room.compiler)
        add("kspIosArm64", libs.androidx.room.compiler)
        add("kspIosX64", libs.androidx.room.compiler)
    }

    // Compose 布局
    implementation(libs.androidx.constraintlayout.compose)

    // Compose UI 核心
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)

    // 测试依赖
//    testImplementation(libs.junit)
//    androidTestImplementation(libs.androidx.test.ext.junit)
//    androidTestImplementation(libs.androidx.test.espresso.core)
//    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
//    debugImplementation(libs.androidx.compose.ui.tooling)
//    debugImplementation(libs.androidx.compose.ui.test.manifest)
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
