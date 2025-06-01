plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    kotlin("kapt")               // KAPT für Room
    alias(libs.plugins.kotlin.compose)

    // Google Services Plugin für Firebase (muss hier angewendet werden, damit google-services.json
    // beim Build ausgewertet wird)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.steppet"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.steppet"
        minSdk = 27
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Room: Schema Export-Pfad festlegen
        javaCompileOptions {
            annotationProcessorOptions {
                arguments += mapOf(
                    "room.schemaLocation" to "$projectDir/schemas"
                )
            }
        }
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

kapt {
    // Auch für KAPT: Room-Schema exportieren
    arguments {
        arg("room.schemaLocation", "$projectDir/schemas")
    }
}

dependencies {
    // ----------------------------
    // Core & Compose
    // ----------------------------
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation("androidx.security:security-crypto:1.0.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.1")
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.lifecycle.process)
    implementation("androidx.work:work-runtime-ktx:2.8.1")
    implementation("androidx.compose.material:material-icons-extended:1.4.0")
    implementation(libs.androidx.navigation.runtime.android)
    implementation(libs.androidx.navigation.compose.android)

    // ----------------------------
    // Room (lokale Datenbank)
    // ----------------------------
    val room_version = "2.7.1"
    implementation("androidx.room:room-runtime:$room_version")
    implementation("androidx.room:room-ktx:$room_version")
    kapt("androidx.room:room-compiler:$room_version")

    // ----------------------------
    // Firebase (Cloud-Backend)
    // ----------------------------
    // 1. Firebase BOM (Version stand Mai 2025, bitte ggf. updaten)
    val firebaseBom = platform("com.google.firebase:firebase-bom:32.1.0")
    implementation(firebaseBom)

    // 2. Firebase Auth (für Login/Logout, z.B. E-Mail/Passwort oder Google-Sign-In)
    implementation("com.google.firebase:firebase-auth-ktx")

    // 3. Cloud Firestore (NoSQL-Datenbank, realtime/offline support)
    implementation("com.google.firebase:firebase-firestore-ktx")

    // 4) Coroutines-Play-Services (damit .await() auf Tasks funktioniert)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.0")

    // (Optional) Firebase Messaging falls Push-Notifications später benötigt werden
    // implementation("com.google.firebase:firebase-messaging-ktx")

    // ----------------------------
    // Testing
    // ----------------------------
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}


