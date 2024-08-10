package eu.darken.octi.kserver

import dagger.BindsInstance
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
        @BindsInstance
        fun config(config: App.Config): Builder

        fun build(): AppComponent
    }
}