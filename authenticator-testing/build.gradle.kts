plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.rick.jetpacksecurityapi37.authenticator.testing"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
}

dependencies {
    api(libs.androidx.security.app.authenticator)
    api(libs.androidx.security.app.authenticator.testing)
}
