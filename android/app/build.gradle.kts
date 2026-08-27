plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.johndraper.flashcardsongs"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.johndraper.flashcardsongs"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "1.2.0"
    }

    signingConfigs {
        create("release") {
            // Keystore is committed on purpose: this is a personal sideload app and a
            // stable signature matters more than keeping the key private. Set the
            // KEYSTORE_* env vars (e.g. from GitHub Secrets) to override.
            storeFile = file("release.keystore")
            storePassword = System.getenv("KEYSTORE_PASSWORD") ?: "flashcards-songs"
            keyAlias = System.getenv("KEY_ALIAS") ?: "flashcards"
            keyPassword = System.getenv("KEY_PASSWORD") ?: "flashcards-songs"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.activity:activity-ktx:1.9.3")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.webkit:webkit:1.11.0")
}

// Bundle the current site (repo-root index.html + songs/) into the APK on every build,
// so the app always ships the same content as the GitHub Pages site.
val syncWebAssets by tasks.registering(Sync::class) {
    from(rootProject.projectDir.parentFile) {
        include("index.html")
        include("songs/**")
    }
    into(layout.projectDirectory.dir("src/main/assets/web"))
}
tasks.named("preBuild") { dependsOn(syncWebAssets) }
