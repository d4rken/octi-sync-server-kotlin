package eu.darken.octi.kserver

import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(
    modules = [

    ]
)
interface AppComponent {
    fun application(): Application

    @Component.Builder
    interface Builder {
        fun build(): AppComponent
    }
}