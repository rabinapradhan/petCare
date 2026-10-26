plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)

    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
}

android {
    namespace = "com.example.petcare"

    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.petcare"

        minSdk = 24
        targetSdk = 35

        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false

            proguardFiles(
                getDefaultProguardFile(
                    "proguard-android-optimize.txt"
                ),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility =
            JavaVersion.VERSION_11

        targetCompatibility =
            JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }
}

secrets {
    propertiesFileName = "secrets.properties"
    defaultPropertiesFileName =
        "local.defaults.properties"
}

dependencies {

    // =========================================================
    // ANDROIDX
    // =========================================================

    implementation(libs.androidx.core.ktx)

    implementation(libs.androidx.activity.ktx)

    implementation(libs.androidx.appcompat)

    implementation(libs.androidx.constraintlayout)


    // =========================================================
    // MATERIAL DESIGN
    // =========================================================

    implementation(libs.material)


    // =========================================================
    // GOOGLE PLACES SDK
    // =========================================================
    //
    // Required for:
    // - Search Nearby
    // - Search by Text
    // - Place details
    // - Kotlin Places APIs
    //

    implementation(
        "com.google.android.libraries.places:places:5.1.1"
    )


    // =========================================================
    // GOOGLE MAPS SDK
    // =========================================================
    //
    // Required for:
    // - GoogleMap
    // - SupportMapFragment
    // - Marker
    // - LatLng
    // - CameraUpdateFactory
    //

    implementation(
        "com.google.android.gms:play-services-maps:20.0.0"
    )


    // =========================================================
    // GOOGLE LOCATION SERVICES
    // =========================================================
    //
    // Required for:
    // - FusedLocationProviderClient
    // - current device location
    //

    implementation(
        "com.google.android.gms:play-services-location:21.4.0"
    )


    // =========================================================
    // ROOM DATABASE
    // =========================================================

    implementation(
        "androidx.room:room-runtime:2.8.4"
    )

    implementation(
        "androidx.room:room-ktx:2.8.4"
    )

    ksp(
        "androidx.room:room-compiler:2.8.4"
    )


    // =========================================================
    // LIFECYCLE
    // =========================================================

    implementation(
        "androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7"
    )


    // =========================================================
    // COROUTINES
    // =========================================================

    implementation(
        "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2"
    )


    // =========================================================
    // TESTING
    // =========================================================

    testImplementation(libs.junit)

    androidTestImplementation(
        libs.androidx.junit
    )

    androidTestImplementation(
        libs.androidx.espresso.core
    )
}