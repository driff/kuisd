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
    implementation(project(":sdui-core"))
    implementation(project(":sdui-compose"))
    implementation(project(":shared"))
    implementation(compose.desktop.currentOs)
    implementation(libs.runtime)
    implementation(libs.foundation)
    implementation(libs.material3)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(kotlin("test"))
}

// Mismo patrón que :desktopApp: el run de Compose Desktop usa el JVM del daemon (Java 21) pero compila
// con la toolchain → apuntamos javaHome a la JDK de la toolchain. Provider lazy (configuration cache).
val runtimeJdkHome =
    javaToolchains
        .launcherFor { languageVersion.set(JavaLanguageVersion.of(jdkVersion)) }
        .map { it.metadata.installationPath.asFile.absolutePath }

compose.desktop {
    application {
        mainClass = "dev.kuisd.builder.MainKt"
        javaHome = runtimeJdkHome.get()
        nativeDistributions {
            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Deb,
            )
            packageName = "KuisdBuilder"
            packageVersion = "1.0.0"
        }
    }
}
