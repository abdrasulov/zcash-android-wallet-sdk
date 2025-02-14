plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("zcash-sdk.android-conventions")
    id("kotlin-kapt")
}

android {
    namespace = "cash.z.ecc.android.sdk.darkside"

    buildTypes {
        create("benchmark") {
            // We provide the extra benchmark build type just for benchmarking purposes
            initWith(buildTypes.getByName("release"))
            matchingFallbacks += listOf("release")
        }
    }
}

dependencies {
    implementation(projects.lightwalletClientLib)
    implementation(projects.sdkLib)
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.multidex)
    implementation(libs.bundles.grpc)

    androidTestImplementation(libs.bundles.androidx.test)

    androidTestImplementation(libs.zcashwalletplgn)
    androidTestImplementation(libs.bip39)
}
