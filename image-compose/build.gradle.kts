plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    `maven-publish`
}

android {
    namespace = "com.padmakar.imageloader.compose"
    compileSdk {
        version = release(37)
    }

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
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

dependencies {
    api(project(":image-core"))
    implementation(project(":image-loader"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                groupId = "com.github.gargpadmakar.KotlinImageLoader"
                artifactId = "image-compose"
                version = "1.0.1"

                pom {
                    name.set("KotlinImageLoader Compose")
                    description.set("Jetpack Compose integration for KotlinImageLoader with AsyncImage, ZoomableAsyncImage, and smooth crossfade transitions.")
                    url.set("https://github.com/gargpadmakar/KotlinImageLoader")
                    licenses {
                        license {
                            name.set("The Apache License, Version 2.0")
                            url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                    }
                    developers {
                        developer {
                            id.set("gargpadmakar")
                            name.set("Padmakar Garg")
                            email.set("gargpadmakar@users.noreply.github.com")
                        }
                    }
                    scm {
                        connection.set("scm:git:git://github.com/gargpadmakar/KotlinImageLoader.git")
                        developerConnection.set("scm:git:ssh://github.com:gargpadmakar/KotlinImageLoader.git")
                        url.set("https://github.com/gargpadmakar/KotlinImageLoader")
                    }
                }
            }
        }
    }
}
