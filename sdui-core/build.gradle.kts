plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvmToolchain(25)

    androidLibrary {
        namespace = "dev.kuisd.sdui.core"
        compileSdk = libs.versions.android.compile.sdk.get().toInt()
        minSdk = libs.versions.android.min.sdk.get().toInt()
        withHostTestBuilder {}
    }

    // Consumido por :server (kotlin jvm) y por shared(desktop). La resolución KMP
    // es por atributo de plataforma (jvm), no por nombre de target.
    jvm()

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { /* lib intermedia: sin binaries.framework propio */ }

    sourceSets {
        commonMain.dependencies {
            // `api`: el contrato expone tipos de kotlinx-serialization en su superficie
            // pública (DefaultSduiJson: Json, SduiComponent.serializer: KSerializer<P>),
            // así que los consumidores deben recibirlos transitivamente.
            api(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
