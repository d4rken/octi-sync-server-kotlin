package eu.darken.octi.kserver.common.serialization

import dagger.Module
import dagger.Provides
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import java.time.Instant
import javax.inject.Singleton

@Module
object SerializationModule {
    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        serializersModule = SerializersModule {
            contextual(Instant::class, InstantSerializer)
        }
    }
}