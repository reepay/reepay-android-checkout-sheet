buildscript {
    val kotlinVersion = "1.7.20"

    repositories {
        google()
        mavenCentral()
        mavenLocal()            // << --- ADD This
    }

    dependencies {
        classpath("com.android.tools.build:gradle:7.1.3")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    }
}


plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("maven-publish")
}

android {
    namespace = "com.billwerk.checkout"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            consumerProguardFiles("consumer-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}


java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

// TODO: may not be needed if using Jitpack
configure<PublishingExtension> {
    publications.create<MavenPublication>("ReepayCheckoutSDK") {
        groupId = "com.github.reepay"
        artifactId = "checkout"
        artifact("$buildDir/outputs/aar/checkout-release.aar")
        version = "1.0.2"
        pom {
            description = "Checkout SDK for Billwerk"
        }
    }
    repositories {
        maven {
            name = "GithubPackages"
            url = uri("https://maven.pkg.github.com/reepay/reepay-android-checkout-sheet")
            credentials {
                username = System.getenv("ANDROID_GITHUB_USER")
                password = System.getenv("ANDROID_GITHUB_TOKEN")
            }
        }
        mavenLocal()
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}

