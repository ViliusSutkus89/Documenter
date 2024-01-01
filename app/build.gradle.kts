plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("androidx.navigation.safeargs.kotlin")
    id("kotlin-kapt")
    id("com.mikepenz.aboutlibraries.plugin")
}

android {
    namespace = "com.viliussutkus89.documenter"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.viliussutkus89.documenter"
        minSdk = 21
        targetSdk = 33

        versionCode = 120
        versionName = "1.0.12"

        if (project.hasProperty("test_build")) {
            versionNameSuffix = ".unreleased"
            applicationIdSuffix = ".unreleased"
        }

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        vectorDrawables.useSupportLibrary = true

        multiDexEnabled = true

        kapt {
            arguments {
                arg("room.schemaLocation", "$projectDir/schemas")
            }
        }
    }
    buildFeatures {
        viewBinding = true
        dataBinding = true
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }

    splits.abi {
        isEnable = true
        // armeabi is deprecated ARM version, supported versions are armeabi-v7a and arm64-v8a,
        // mips support is long gone too,
        // risc is not supported yet
        exclude("armeabi", "mips", "mips64", "riscv64")
        isUniversalApk = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            ndk.debugSymbolLevel = "FULL"

            val signingKeyfile: String? = System.getenv("SIGNING_KEYFILE")
            val signingAlias: String? = System.getenv("SIGNING_ALIAS")
            val signingPass: String? = System.getenv("SIGNING_PASS")
            if (!listOf(signingKeyfile, signingAlias, signingPass).contains(null)) {
                signingConfigs.getByName("release") {
                    storeFile = file(System.getenv("SIGNING_KEYFILE"))
                    storePassword = System.getenv("SIGNING_PASS")
                    keyAlias = System.getenv("SIGNING_ALIAS")
                    keyPassword = System.getenv("SIGNING_PASS")
                }
                signingConfig = signingConfigs.getByName("release")
            }
        }
        debug {
            versionNameSuffix = ".debug"
            applicationIdSuffix = ".debug"
        }
    }

    packagingOptions.jniLibs {
        pickFirsts.add("**/libc++_shared.so")
        keepDebugSymbols.add("**/libpdf2htmlEX-android.so")
        keepDebugSymbols.add("**/libwvware-android.so")
    }
    ndkVersion = "26.1.10909125"

    lint.abortOnError = false
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    implementation("androidx.multidex:multidex:2.0.1")

    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")

    implementation("androidx.navigation:navigation-fragment-ktx:2.7.6")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.6")

    implementation("androidx.work:work-runtime:2.9.0")
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("androidx.work:work-multiprocess:2.9.0")
    implementation("androidx.concurrent:concurrent-futures:1.1.0")
    implementation("androidx.preference:preference:1.2.1")

    kapt("androidx.room:room-compiler:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")

    implementation("androidx.webkit:webkit:1.9.0")

    implementation("com.viliussutkus89:pdf2htmlex-android:0.18.22")
    implementation("com.viliussutkus89:wvware-android:1.2.8")

    implementation("com.mikepenz:aboutlibraries:10.10.0")

    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.ext:junit-ktx:1.1.5")

    androidTestImplementation("androidx.test:core-ktx:1.5.0")
    androidTestImplementation("androidx.test:runner:1.5.0")
    androidTestImplementation("androidx.test:rules:1.5.0")

    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.test.espresso:espresso-intents:3.5.1")
    implementation("androidx.test.espresso:espresso-idling-resource:3.5.1")
    androidTestImplementation("androidx.test.espresso:espresso-contrib:3.5.1")

    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.2.0")

    androidTestImplementation("com.viliussutkus89:assetextractor-android:1.3.3")
}
