plugins {
    alias(libs.plugins.android.library)
    `maven-publish`
}

android {
    namespace = "com.padmakar.imageloader.data"
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
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

dependencies {
    implementation(project(":image-core"))
    implementation(project(":image-domain"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.androidx.core.ktx)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                groupId = "com.github.gargpadmakar.KotlinImageLoader"
                artifactId = "image-data"
                version = "1.0.1"

                pom {
                    name.set("KotlinImageLoader Data")
                    description.set("Data layer, network fetchers, and decoders for KotlinImageLoader.")
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
