import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
            export(projects.core)
            export(projects.feature.auth)
            export(projects.feature.barcode)
            export(projects.feature.household)
            export(projects.feature.notifications)
            export(projects.feature.products)
            export(projects.feature.profile)
            export(projects.feature.recommendations)
        }
    }

    jvm()

    sourceSets {
        commonMain.dependencies {
            api(projects.core)
            api(projects.feature.auth)
            api(projects.feature.barcode)
            api(projects.feature.household)
            api(projects.feature.notifications)
            api(projects.feature.products)
            api(projects.feature.profile)
            api(projects.feature.recommendations)
            implementation(libs.koin.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace = "com.android.rut.miit.productinventory.shared"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
}
