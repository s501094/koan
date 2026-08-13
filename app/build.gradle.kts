import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// Release signing credentials live outside the repo. Put them in
// ~/.gradle/gradle.properties or ~/.koan/signing.properties.
val signingProps = Properties().apply {
    val f = File(System.getProperty("user.home"), ".koan/signing.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

fun signingValue(key: String): String? =
    signingProps.getProperty(key) ?: providers.gradleProperty(key).orNull

android {
    namespace = "com.tyell.koan"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.tyell.koan"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

        // Gecko's libxul.so is ~152MB per ABI, already stripped upstream — that
        // is simply what the engine costs, and it dominates the APK. One ABI only.
        // Every phone worth targeting is arm64; add armeabi-v7a for an old device.
        ndk {
            abiFilters += "arm64-v8a"
        }
    }

    signingConfigs {
        create("release") {
            val storePath = signingValue("koanStoreFile")
            if (storePath != null && File(storePath).exists()) {
                storeFile = File(storePath)
                storePassword = signingValue("koanStorePassword")
                keyAlias = signingValue("koanKeyAlias")
                keyPassword = signingValue("koanKeyPassword")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // Falls back to unsigned if the keystore isn't set up yet.
            signingConfig = signingConfigs.getByName("release")
                .takeIf { it.storeFile != null }
        }
    }

    buildFeatures {
        compose = true
    }

    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
        resources {
            excludes += setOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "/META-INF/*.kotlin_module",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        abortOnError = false
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

/*
 * Things that arrive uninvited.
 *
 * androidx.test: org.mozilla.components:concept-toolbar (a production artifact)
 * depends on androidx.test.ext:junit-ktx, which drags JUnit and three
 * instrumentation activities into the shipped APK. That is a packaging bug
 * upstream, not something we need.
 *
 * com.google.android.gms is deliberately NOT excluded, though it was on a first
 * pass. GeckoView's own org.mozilla.geckoview.WebAuthnTokenManager references
 * com.google.android.gms.fido directly, and Gecko reaches it for plain feature
 * detection — sites call isUserVerifyingPlatformAuthenticatorAvailable() on page
 * load. Removing the library turns that into a NoClassDefFoundError on ordinary
 * pages, so the exclusion traded a dormant library for real breakage.
 *
 * What that library is: an on-device client for the platform FIDO/passkey API.
 * It is not analytics, it makes no network calls of its own, and it only runs
 * when a site asks for WebAuthn. To be rid of it entirely, set
 * `security.webauth.webauthn` to false in about:config — that stops Gecko
 * entering the code path at all, at the cost of passkey support.
 */
configurations.configureEach {
    exclude(group = "androidx.test")
    exclude(group = "androidx.test.ext")
    exclude(group = "androidx.test.services")
    exclude(group = "androidx.test.espresso")
}

dependencies {
    implementation(project(":core:engine"))
    implementation(project(":core:data"))
    implementation(project(":core:theme"))
    implementation(project(":core:design"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.datastore.preferences)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    testImplementation(libs.junit)
}
