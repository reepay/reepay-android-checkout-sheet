import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.gradle.api.publish.maven.MavenPublication
import groovy.util.Node

buildscript {
    val kotlinVersion = "2.2.20"

    repositories {
        google()
        mavenCentral()
        mavenLocal()
    }

    dependencies {
        classpath("com.android.tools.build:gradle:8.13.0")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    }
}


plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("maven-publish")
}

version = "1.0.24"

android {
    namespace = "com.billwerk.checkout"
    compileSdk = 36

    defaultConfig {
        resValue("string", "library_version", version.toString())

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

    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    publishing {
        singleVariant("release") {

        }
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

dependencies {
    api("androidx.webkit:webkit:1.14.0")
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.google.code.gson:gson:2.13.2")
    implementation("com.google.android.material:material:1.13.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                groupId = "com.github.reepay"
                artifactId = "reepay-android-checkout-sheet"
                version = project.version.toString()

                val webkitDep = project.configurations
                    .getByName("api")
                    .dependencies
                    .find { it.group == "androidx.webkit" && it.name == "webkit" }

                if (webkitDep == null) {
                    logger.warn("⚠️ No WebKit dependency found in api configuration; POM will not include it.")
                }

                pom.withXml {
                    val root = asNode()
                    val depsList = (root.get("dependencies") as? groovy.util.NodeList)?.toList()
                    val existingDeps = depsList?.firstOrNull() as? Node
                        ?: root.appendNode("dependencies")

                    val alreadyPresent = existingDeps.children()
                        .filterIsInstance<Node>()
                        .any {
                            (it.get("groupId") as? List<*>)?.firstOrNull()
                                ?.toString() == "androidx.webkit" &&
                                    (it.get("artifactId") as? List<*>)?.firstOrNull()
                                        ?.toString() == "webkit"
                        }

                    if (!alreadyPresent && webkitDep != null) {
                        val depNode = existingDeps.appendNode("dependency")
                        depNode.appendNode("groupId", webkitDep.group)
                        depNode.appendNode("artifactId", webkitDep.name)
                        depNode.appendNode("version", webkitDep.version)
                        depNode.appendNode("scope", "compile")
                    }
                }
            }
        }
    }
}