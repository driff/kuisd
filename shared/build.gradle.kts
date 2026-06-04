plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvmToolchain(25)

    androidLibrary {
        namespace = "dev.kuisd.shared"
        compileSdk = libs.versions.android.compile.sdk.get().toInt()
        minSdk = libs.versions.android.min.sdk.get().toInt()
        withHostTestBuilder {}
    }

    jvm("desktop")

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { target ->
        target.binaries.framework {
            baseName = "shared"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            // El motor (trae :sdui-core + Compose runtime/foundation/material3/ui transitivos
            // por `api`). `implementation`: las apps host (:androidApp/:desktopApp) consumen
            // :shared para arrancar la app, no para escribir componentes SDUI.
            implementation(project(":sdui-compose"))
            // La app usa tipos de core directamente (SduiEnvelope, UiAction, Navigate…):
            // dependencia directa = declarada explícitamente, aunque llegue transitiva.
            implementation(project(":sdui-core"))
            implementation(libs.ktor.client.core)
            implementation(libs.kotlinx.coroutines.core)
            // Coil 3 (spec 012): impl del seam LocalAsyncImage; usa Ktor 3 para la red.
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor3)
            // Compose Multiplatform Resources (spec 013): painterResource + Res para assets locales.
            implementation(compose.components.resources)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        val desktopMain by getting {
            dependencies {
                implementation(libs.ktor.client.cio)
            }
        }
    }
}

// Compose Resources (spec 013): paquete fijo del `Res` generado para imports estables.
compose.resources {
    publicResClass = true
    packageOfResClass = "dev.kuisd.shared.resources"
}
