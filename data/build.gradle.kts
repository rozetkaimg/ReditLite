plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.kotlinSerialization)
    id("com.google.devtools.ksp") version "2.3.6"
    id("androidx.room") version "2.8.4"
}

room {
    schemaDirectory("$projectDir/schemas")
}

kotlin {
    androidLibrary {
        namespace = "com.rozetka.data"
        compileSdk = 36
        minSdk = 24
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            api(project(":domain"))

            implementation(libs.koin.core)
            api(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.auth)
            implementation(libs.ktor.client.logging)
            api(libs.androidx.room.runtime)
            implementation(libs.androidx.sqlite.bundled)
            implementation(libs.multiplatform.settings)
        }

        androidMain.dependencies {

            implementation(libs.ktor.client.okhttp)
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
    }
}

dependencies {
    val roomCompiler = "androidx.room:room-compiler:2.8.4"
    add("kspAndroid", roomCompiler)
    add("kspIosSimulatorArm64", roomCompiler)
    add("kspIosArm64", roomCompiler)
    add("kspIosX64", roomCompiler)
}