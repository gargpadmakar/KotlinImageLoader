plugins {
    alias(libs.plugins.android.library)
    `maven-publish`
}

android {
    namespace = "com.padmakar.imageloader.loader"
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
    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

dependencies {
    api(project(":image-core"))
    implementation(project(":image-domain"))
    implementation(project(":image-data"))

    implementation(libs.kotlinx.coroutines.core)

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
                artifactId = "image-loader"
                version = "1.0.1"

                pom {
                    name.set("KotlinImageLoader Engine")
                    description.set("Production-ready Android image loading engine with multi-tier LRU caching, request deduplication, and decoding pipelines.")
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
