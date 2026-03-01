import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("com.chaquo.python")
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

android {
    namespace = "com.jingtian.composedemo"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.jingtian.composedemo"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        ndk {
            abiFilters += listOf("arm64-v8a", "x86_64")
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            resValue("string", "app_name", "${configProps.getProperty("APP_NAME")}.debug")
        }
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            resValue("string", "app_name", "${configProps.getProperty("APP_NAME")}")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
        viewBinding = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.3"
    }
    packagingOptions {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
//    flavorDimensions += "pyVersion"
//    productFlavors {
//        create("py39") { dimension = "pyVersion" }
//        create("py310") { dimension = "pyVersion" }
//        create("py311") { dimension = "pyVersion" }
//        create("py312") { dimension = "pyVersion" }
//    }
}
//执行python 接入文档: https://chaquo.com/chaquopy/doc/15.0/android.html
chaquopy {
//    productFlavors {
//        getByName("py39") { version = "3.9" }
//        getByName("py310") { version = "3.10" }
//        getByName("py311") { version = "3.11" }
//        getByName("py312") { version = "3.12" }
//    }
    defaultConfig {
        version = "3.9"
        pip {
//            // A requirement specifier, with or without a version number:
//            install("scipy")
//            install("requests==2.24.0")
//
//            // An sdist or wheel filename, relative to the project directory:
//            install("MyPackage-1.2.3-py2.py3-none-any.whl")
//
//            // A directory containing a setup.py, relative to the project
//            // directory (must contain at least one slash):
//            install("./MyPackage")
//
//            // "-r"` followed by a requirements filename, relative to the
//            // project directory:
//            install("-r", "requirements.txt")
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.8.0")
    implementation("androidx.appcompat:appcompat:1.4.1")
//    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
    // activity-compose 版本需适配 Compose 和 AGP，1.4.0 是 AGP 7.0.4 兼容的稳定版
    implementation("androidx.activity:activity-compose:1.8.0")
    implementation("androidx.recyclerview:recyclerview:1.2.0")
    implementation("androidx.documentfile:documentfile:1.0.0")

    val composeVersion = "1.6.0"

    implementation("androidx.compose.ui:ui:$composeVersion")
    implementation("androidx.compose.ui:ui-graphics:$composeVersion")
    implementation("androidx.compose.ui:ui-tooling-preview:$composeVersion")
    implementation("androidx.compose.material3:material3:1.2.0")

    // 测试依赖（版本适配）
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:$composeVersion")
    debugImplementation("androidx.compose.ui:ui-tooling:$composeVersion")
    debugImplementation("androidx.compose.ui:ui-test-manifest:$composeVersion")

    val room_version = "2.6.1"
    implementation("androidx.room:room-runtime:$room_version")
    ksp("androidx.room:room-compiler:$room_version")
    implementation("androidx.room:room-ktx:$room_version")
    implementation("androidx.room:room-rxjava2:$room_version")
    implementation("androidx.room:room-paging:$room_version")

    implementation("com.google.code.gson:gson:2.8.8")


    implementation("androidx.lifecycle:lifecycle-viewmodel:2.5.1")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.5.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.5.1")
    implementation("androidx.compose.runtime:runtime-livedata:1.0.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.5.1")

    implementation("androidx.constraintlayout:constraintlayout-compose:1.0.1")
}