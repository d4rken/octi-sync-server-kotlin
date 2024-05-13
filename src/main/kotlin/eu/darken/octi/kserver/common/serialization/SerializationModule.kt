package eu.darken.octi.kserver.common.serialization

import dagger.Module
import dagger.Provides
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import java.time.Instant
import java.util.*
import javax.inject.Singleton

@Module
object SerializationModule {
    @Provides
    @Singleton
    fun provideJson(serializersModule: SerializersModule): Json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        this.serializersModule = serializersModule
    }

    @Provides
    @Singleton
    fun provideSerializerModule(): SerializersModule = SerializersModule {
        contextual(Instant::class, InstantSerializer)
        contextual(UUID::class, UUIDSerializer)
    }
}