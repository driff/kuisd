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
        namespace = "dev.kuisd.sdui"
        compileSdk = libs.versions.android.compile.sdk.get().toInt()
        minSdk = libs.versions.android.min.sdk.get().toInt()
        withHostTestBuilder {}
    }

    // Consumido por shared(desktop). La resolución KMP es por atributo de plataforma
    // (jvm), no por nombre de target, así que `jvm()` cubre `jvm("desktop")` de :shared.
    jvm()

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { /* lib intermedia: sin binaries.framework propio (como :sdui-core) */ }

    sourceSets {
        commonMain.dependencies {
            // `api`: el motor expone tipos del contrato (SduiNode, SduiComponent<P>,
            // DefaultSduiJson) y tipos Compose (@Composable RenderScope.(P) -> Unit, los
            // Local*) en su superficie pública, así que los consumidores (p.ej. :shared)
            // deben recibirlos transitivamente.
            api(project(":sdui-core"))
            api(libs.runtime)
            api(libs.foundation)
            api(libs.material3)
            api(libs.ui)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
