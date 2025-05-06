plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id ("androidx.navigation.safeargs.kotlin")
    id("com.google.gms.google-services")
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "mx.edu.itesca.happybox_10"
    compileSdk = 35

    defaultConfig {
        applicationId = "mx.edu.itesca.happybox_10"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures{
        compose=true // Activa el soporte de Jetpack Compose
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.15"
    }

    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures{
        viewBinding=true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.firebase.storage.ktx)
    implementation(libs.firebase.functions.ktx)
    implementation(libs.firebase.appcheck.debug)
    implementation(libs.billing.ktx)
    implementation(libs.androidx.swiperefreshlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    //Splash
    implementation ("androidx.core:core-splashscreen:1.0.1")
    //Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.11.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-analytics")
    implementation ("com.google.firebase:firebase-firestore")
    implementation ("com.google.firebase:firebase-auth-ktx:23.2.0")
    implementation ("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    //Google
    implementation("com.google.android.material:material:1.13.0-alpha12")
    implementation("androidx.credentials:credentials:1.5.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.5.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    implementation("androidx.viewpager2:viewpager2:1.1.0")
    implementation ("com.github.bumptech.glide:glide:4.16.0")

    // Corrutinas para lifecycleScope
    implementation ("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")

    implementation("androidx.compose.ui:ui-android:1.7.8") //LocalContext
    implementation("androidx.activity:activity-compose:1.9.3") //setContext

    //toasty
    implementation("com.github.GrenderG:Toasty:1.5.2")
    implementation("androidx.appcompat:appcompat:1.6.1")

    //Stripe SDK
    implementation ("com.stripe:stripe-android:21.12.0")

    // PlayIntegrity de pago
    implementation("com.google.firebase:firebase-appcheck-playintegrity")
    // PlayIntegrity de desarrollo (gratuito)
    implementation("com.google.firebase:firebase-appcheck-debug")



}