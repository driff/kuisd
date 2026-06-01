import org.gradle.jvm.toolchain.JavaLanguageVersion

plugins {
    kotlin("jvm")
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    jvmToolchain(25)
}

dependencies {
    implementation(project(":shared"))
    implementation(compose.desktop.currentOs)
    implementation(libs.runtime)
    implementation(libs.foundation)
    implementation(libs.material3)
}

// El `run`/empaquetado de Compose Desktop usa por defecto el JVM del daemon Gradle (Java 21, fijado
// en gradle/gradle-daemon-jvm.properties), pero las clases se compilan con la toolchain 25 → mismatch
// (UnsupportedClassVersionError). Apuntamos `javaHome` a la JDK 25 resuelta por la toolchain service.
val runtimeJdk25: String =
    javaToolchains
        .launcherFor { languageVersion.set(JavaLanguageVersion.of(25)) }
        .get()
        .metadata
        .installationPath
        .asFile
        .absolutePath

compose.desktop {
    application {
        mainClass = "dev.kuisd.desktop.MainKt"
        javaHome = runtimeJdk25
        nativeDistributions {
            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Deb,
            )
            packageName = "Kuisd"
            packageVersion = "1.0.0"
        }
    }
}
