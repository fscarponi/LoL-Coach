package com.lolcoach.bridge.client

import com.lolcoach.bridge.model.LockfileData
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.websocket.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager

object KtorClientFactory {

    private val lenientJson = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val trustAllManager = object : X509TrustManager {
        override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
        override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
        override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
    }

    fun createLcuClient(lockfileData: LockfileData): HttpClient {
        return HttpClient(CIO) {
            engine {
                https {
                    trustManager = trustAllManager
                }
            }
            install(ContentNegotiation) {
                json(lenientJson)
            }
            install(Auth) {
                basic {
                    credentials {
                        BasicAuthCredentials(username = "riot", password = lockfileData.token)
                    }
                    sendWithoutRequest { true }
                }
            }
            install(WebSockets)
        }
    }

    fun createLiveClientDataClient(): HttpClient {
        return HttpClient(CIO) {
            engine {
                https {
                    trustManager = trustAllManager
                }
            }
            install(ContentNegotiation) {
                json(lenientJson)
            }
        }
    }
}
