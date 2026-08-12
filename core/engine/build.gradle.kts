plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.tyell.koan.engine"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

// geckoview-omni bundles its own Glean native library and declares the
// `glean-native` capability for it. Anything pulling org.mozilla.telemetry:glean
// declares the same capability at a different version, and Gradle refuses to
// pick. Gecko's copy has to win — it's the one actually loaded into the process.
configurations.configureEach {
    resolutionStrategy.capabilitiesResolution.withCapability("org.mozilla.telemetry:glean-native") {
        selectHighestVersion()
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)

    // GeckoView arrives transitively via browser-engine-gecko -> geckoview-omni.
    api(libs.mozac.browser.engine.gecko)
    api(libs.mozac.browser.state)
    api(libs.mozac.concept.engine)
    api(libs.mozac.concept.fetch)
    api(libs.mozac.browser.session.storage)
    api(libs.mozac.browser.icons)
    api(libs.mozac.feature.session)
    api(libs.mozac.feature.tabs)
    api(libs.mozac.support.ktx)
    api(libs.mozac.support.base)
    api(libs.mozac.lib.state)

    testImplementation(libs.junit)
}
