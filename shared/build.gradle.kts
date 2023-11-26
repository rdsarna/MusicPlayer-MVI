plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinCocoapods)
    alias(libs.plugins.androidLibrary)
}

@OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
kotlin {
    targetHierarchy.default()
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
            }
        }
    }
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    cocoapods {
        summary = "Some description for the Shared Module"
        homepage = "Link to the Shared Module homepage"
        version = "1.0"
        ios.deploymentTarget = "16.0"
        framework {
            baseName = "shared"
            isStatic = true
        }
    }

    sourceSets {
        sourceSets["commonMain"].dependencies {
            api(libs.koin.core)
        }
        sourceSets["commonTest"].dependencies {
            implementation(kotlin("test"))
        }
        sourceSets["androidMain"].dependencies {
            implementation(libs.koin.android)
        }
    }
}

android {
    namespace = "com.ratulsarna.shared"
    compileSdk = 34
    defaultConfig {
        minSdk = 24
    }
}