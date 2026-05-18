import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.googleServices)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    
    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.koin.android)
            implementation(libs.ktor.client.core)
            implementation(libs.androidx.navigation.compose)
            implementation(libs.androidx.room.runtime)
            implementation(libs.androidx.room.ktx)
            implementation(libs.camerax.camera2)
            implementation(libs.camerax.lifecycle)
            implementation(libs.camerax.view)
            implementation(libs.mlkit.barcode.scanning)
            implementation(libs.coil.compose)
            implementation(libs.coil.network.okhttp)
            implementation("com.google.guava:guava:33.4.0-android")
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.koin.core)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.serialization.json)
            implementation(projects.shared)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        androidUnitTest.dependencies {
            implementation(libs.junit)
            implementation("androidx.compose.ui:ui-test-junit4:1.10.5")
            implementation("androidx.room:room-testing:2.7.1")
            implementation("androidx.test:core:1.6.1")
            implementation("org.robolectric:robolectric:4.13")
        }
    }
}

android {
    namespace = "com.android.rut.miit.productinventory"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.android.rut.miit.productinventory"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    debugImplementation(libs.compose.uiTooling)
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.10.5")
    add("kspAndroid", libs.androidx.room.compiler)
}
