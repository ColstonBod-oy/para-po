plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)

    // Add the Google services Gradle plugin
    id("com.google.gms.google-services")
}

android {
    namespace = "com.enigma.parapo"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.enigma.parapo"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.play.services.location)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Import the Firebase BoM
    implementation(platform("com.google.firebase:firebase-bom:32.7.4"))

    // Add the dependency for the Firebase Authentication library
    // When using the BoM, you don't specify versions in Firebase library dependencies
    implementation("com.google.firebase:firebase-auth")

    // Declare the dependency for the Cloud Firestore library
    implementation("com.google.firebase:firebase-firestore")

    // Add the dependency for the Cloud Storage library
    implementation("com.google.firebase:firebase-storage")

    // Also add the dependency for the Google Play services library and specify its version
    implementation("com.google.android.gms:play-services-auth:21.0.0")

    // Dependency to use circular image view
    implementation("de.hdodenhof:circleimageview:3.1.0")

    // Dependency for loading url images
    implementation("com.squareup.picasso:picasso:2.8")

    // Dependency for the bottom navbar
    implementation("np.com.susanthapa:curved_bottom_navigation:0.6.5")

    // Dependencies for nav controller
    implementation("androidx.navigation:navigation-fragment-ktx:2.3.5")
    implementation("androidx.navigation:navigation-ui-ktx:2.3.5")
    implementation("androidx.lifecycle:lifecycle-extensions:2.2.0")

    // Mapbox
    implementation("com.mapbox.search:mapbox-search-android-ui:2.0.0-beta.2")
    implementation("com.mapbox.maps:android:11.0.0")
    implementation("com.mapbox.navigationcore:android:3.0.0-rc.5")

    // StickerView
    implementation(project(":stickerview"))
}