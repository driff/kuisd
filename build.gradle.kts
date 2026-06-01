import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication

plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.sqldelight) apply false
    alias(libs.plugins.ktor) apply false
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
}

allprojects {
    apply(plugin = rootProject.libs.plugins.detekt.get().pluginId)
    apply(plugin = rootProject.libs.plugins.ktlint.get().pluginId)

    detekt {
        config.setFrom(rootProject.files("config/detekt/detekt.yml"))
        buildUponDefaultConfig = true
        autoCorrect = false
    }
    // NOTE: detekt-formatting ruleset is deferred — not yet published at 2.0.0-alpha.3.
    // The base ruleset (which includes ForbiddenImport, our Clean Architecture rule) is enough.
    // ktlint covers the formatting concerns formatting-ruleset would have caught.
}

// ---------------------------------------------------------------------------
// Convención de publicación Maven para las librerías SDUI reutilizables.
// Se aplica automáticamente a cualquier módulo cuyo nombre empiece por `sdui-`
// (hoy :sdui-core; mañana :sdui-compose / :sdui-ktor la heredan sin tocar nada).
//
//   ./gradlew publishToMavenLocal                 -> publica a ~/.m2 (probar local)
//   ./gradlew publishAllPublicationsToRemoteRepository \
//       -Pkuisd.publish.url=... -Pkuisd.publish.user=... -Pkuisd.publish.password=...
//
// Coordenadas: dev.kuisd:<módulo>:<versión>  (versión vía -Pkuisd.version, def. 0.1.0)
// ---------------------------------------------------------------------------
subprojects {
    if (!name.startsWith("sdui-")) return@subprojects

    apply(plugin = "maven-publish")

    group = "dev.kuisd"
    version = (findProperty("kuisd.version") as String?) ?: "0.1.0"

    // Los módulos KMP (kotlin.multiplatform) generan sus publicaciones por target
    // automáticamente. Los módulos JVM puros (p.ej. el futuro :sdui-ktor) necesitan
    // que creemos la publicación desde el componente `java`.
    plugins.withId("org.jetbrains.kotlin.jvm") {
        extensions.configure<PublishingExtension> {
            publications {
                create<MavenPublication>("maven") {
                    from(components["java"])
                }
            }
        }
    }

    extensions.configure<PublishingExtension> {
        repositories {
            // mavenLocal siempre disponible vía la tarea `publishToMavenLocal`.
            val remoteUrl = findProperty("kuisd.publish.url") as String?
            if (remoteUrl != null) {
                maven {
                    name = "remote"
                    url = uri(remoteUrl)
                    credentials {
                        username = findProperty("kuisd.publish.user") as String?
                        password = findProperty("kuisd.publish.password") as String?
                    }
                }
            }
        }
        // POM común para todas las publicaciones (las de KMP se añaden tarde: configureEach).
        publications.withType<MavenPublication>().configureEach {
            pom {
                name.set(artifactId)
                description.set("kuisd · librería SDUI ($artifactId)")
                url.set("https://github.com/jjgp/kuisd")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
            }
        }
    }
}
