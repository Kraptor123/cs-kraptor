import com.lagradost.cloudstream3.gradle.CloudstreamExtension
import com.android.build.gradle.BaseExtension

buildscript {
    repositories {
        google()
        mavenCentral()
        // Shitpack repo which contains our tools and dependencies
        maven("https://jitpack.io")
    }

    dependencies {
        classpath("com.android.tools.build:gradle:8.7.3")
        // Cloudstream gradle plugin which makes everything work and builds plugins
        classpath("com.github.recloudstream:gradle:master-SNAPSHOT")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.1.0")
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}

fun Project.cloudstream(configuration: CloudstreamExtension.() -> Unit) = extensions.getByName<CloudstreamExtension>("cloudstream").configuration()

fun Project.android(configuration: BaseExtension.() -> Unit) = extensions.getByName<BaseExtension>("android").configuration()

subprojects {
    apply(plugin = "com.android.library")
    apply(plugin = "kotlin-android")
    apply(plugin = "com.lagradost.cloudstream3.gradle")

    cloudstream {
        // when running through github workflow, GITHUB_REPOSITORY should contain current repository name
        setRepo(System.getenv("GITHUB_REPOSITORY") ?: "https://github.com/Kraptor123/cs-kekikanime")

        authors = listOf("kraptor")
    }

    android {
        namespace = "com.kraptor"

        defaultConfig {
            minSdk = 21
            compileSdkVersion(35)
            targetSdk = 35
        }

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_1_8
            targetCompatibility = JavaVersion.VERSION_1_8
        }

        tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile> {
            compilerOptions {
                jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8)
                freeCompilerArgs.addAll(
                    listOf(
                        "-Xno-call-assertions",
                        "-Xno-param-assertions",
                        "-Xno-receiver-assertions"
                    )
                )
            }
        }
    }


    dependencies {
        val cloudstream by configurations
        val implementation by configurations

        // Stubs for all Cloudstream classes
        cloudstream("com.lagradost:cloudstream3:pre-release")

        // these dependencies can include any of those which are added by the app,
        // but you dont need to include any of them if you dont need them
        // https://github.com/recloudstream/cloudstream/blob/master/app/build.gradle
        implementation(kotlin("stdlib"))                                              // Kotlin'in temel kütüphanesi
        implementation("com.github.Blatzar:NiceHttp:0.4.11")                          // HTTP kütüphanesi
        implementation("org.jsoup:jsoup:1.18.3")                                      // HTML ayrıştırıcı
        implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.1")   // Kotlin için Jackson JSON kütüphanesi
        implementation("com.fasterxml.jackson.core:jackson-databind:2.16.0")          // JSON-nesne dönüştürme kütüphanesi
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")      // Kotlin için asenkron işlemler
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")
    }
}

task<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}

// CS3 dosyalarını taşıyan task
tasks.register("moveCs3Files") {
    doLast {
        // Tüm subprojectlerin build klasörlerini kontrol et
        subprojects.forEach { subproject ->
            val buildDir = subproject.layout.buildDirectory.get().asFile
            if (buildDir.exists()) {
                buildDir.walkTopDown().forEach { file ->
                    if (file.extension == "cs3") {
                        println("Moving: ${file.name}")

                        // Hedef dosya yolu (ana dizin)
                        val targetFile = File(rootProject.projectDir, file.name)

                        // Dosyayı kopyala (varsa üzerine yaz)
                        file.copyTo(targetFile, overwrite = true)

                        println("Moved ${file.name} to ${targetFile.absolutePath}")
                    }
                }
            }
        }
        println("All .cs3 files moved to root directory")
    }
}

// Subprojectlerdeki make task'larını çalıştır ve CS3 dosyalarını taşı
tasks.register("buildAndMove") {
    dependsOn(subprojects.map { "${it.path}:make" })
    finalizedBy("moveCs3Files")
}

// Alternatif olarak sadece makePluginsJson kullanabilirsiniz
tasks.register("buildAndMovePlugins") {
    dependsOn(subprojects.map { "${it.path}:makePluginsJson" })
    finalizedBy("moveCs3Files")
}