package eu.darken.octi.kserver

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import java.nio.file.Files
import java.nio.file.Path
import kotlin.concurrent.thread
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.Path
import kotlin.io.path.deleteRecursively

@OptIn(ExperimentalPathApi::class)
abstract class BaseServerTest {

    val dataPath = Path("./build/tmp/testdatapath")
    lateinit var client: HttpClient

    private lateinit var app: App

    private val config = App.Config(
        dataPath = dataPath,
        port = 8080,
        isDebug = true,
        useRateLimit = false,
    )

    @BeforeEach
    fun before() {
        if (Files.exists(dataPath)) dataPath.deleteRecursively()
        Files.createDirectories(dataPath)

        app = DaggerAppComponent.builder().config(config).build().application()
        thread { app.launch() }

        while (!app.isRunning()) Thread.sleep(100)

        client = HttpClient(CIO) {
            defaultRequest {
                url {
                    protocol = URLProtocol.HTTP
                    host = "localhost"
                    port = config.port
                }
            }
        }
    }

    @AfterEach
    fun after() {
        client.close()
        app.shutdown()
        dataPath.deleteRecursively()
    }

    fun runTest2(test: suspend HttpClient.() -> Unit) = runTest {
        test(client)
    }

    fun Credentials.getAccountPath(): Path {
        return dataPath.resolve("accounts").resolve(this.account)
    }
}