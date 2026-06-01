package dev.kuisd.sdui.core

import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.plus

fun sduiJson(extra: SerializersModule = EmptySerializersModule()): Json = Json {
    classDiscriminator = "type"
    encodeDefaults = false
    ignoreUnknownKeys = true
    explicitNulls = false
    serializersModule = extra + SerializersModule {
        polymorphicDefaultDeserializer(UiAction::class) { NoOpAction.serializer() }
    }
}

val DefaultSduiJson: Json = sduiJson()
