// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.6.0" apply false
    // w: Kapt currently doesn't support language version 2.0+. Falling back to 1.9.
    //noinspection GradleDependency
    id("org.jetbrains.kotlin.android") version "1.9.25" apply false
    //noinspection GradleDependency
    id("com.google.devtools.ksp") version "1.9.25-1.0.20" apply false
    id("com.mikepenz.aboutlibraries.plugin") version "10.10.0" apply false
    id("androidx.navigation.safeargs.kotlin") version "2.7.7" apply false
}
