plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinCocoapods)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.skie)
}

kotlin {
    applyDefaultHierarchyTemplate()
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
        podfile = project.file("../iosApp/Podfile")
        framework {
            baseName = "shared"
            isStatic = true
        }
    }

    sourceSets {
        all {
            languageSettings.optIn("kotlinx.cinterop.ExperimentalForeignApi")
        }
        sourceSets["commonMain"].dependencies {
            api(libs.koin.core)
            api(libs.kmm.viewmodel.core)
            implementation(libs.kotlinx.collections.immutable)
            implementation(libs.skie.config.annotations)
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