package eu.darken.octi.kserver

import dagger.Component
import eu.darken.octi.kserver.common.serialization.SerializationModule
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        SerializationModule::class
    ]
)
interface AppComponent {
    fun application(): App

    @Component.Builder
    interface Builder {
        fun build(): AppComponent
    }
}