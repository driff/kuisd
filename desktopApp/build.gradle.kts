import org.gradle.jvm.toolchain.JavaLanguageVersion

plugins {
    kotlin("jvm")
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

// Versión única de la toolchain para este módulo (evita drift entre jvmToolchain y el javaHome del run).
val jdkVersion = 25

kotlin {
    jvmToolchain(jdkVersion)
}

dependencies {
    implementation(project(":shared"))
    implementation(compose.desktop.currentOs)
    implementation(libs.runtime)
    implementation(libs.foundation)
    implementation(libs.material3)
}

// El `run`/empaquetado de Compose Desktop usa por defecto el JVM del daemon Gradle (Java 21, fijado
// en gradle/gradle-daemon-jvm.properties), pero las clases se compilan con la toolchain → mismatch
// (UnsupportedClassVersionError). Apuntamos `javaHome` a la JDK de la toolchain. Provider lazy para
// no resolver la toolchain a nivel de script (configuration cache); el .get() se difiere al bloque.
val runtimeJdkHome =
    javaToolchains
        .launcherFor { languageVersion.set(JavaLanguageVersion.of(jdkVersion)) }
        .map { it.metadata.installationPath.asFile.absolutePath }

compose.desktop {
    application {
        mainClass = "dev.kuisd.desktop.MainKt"
        javaHome = runtimeJdkHome.get()
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
