/*
 * Copyright (C) 2024-2025 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

// @formatter:off
package me.him188.ani.client.infrastructure

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.Parameters
import io.ktor.http.URLBuilder
import io.ktor.http.content.PartData
import io.ktor.http.contentType
import io.ktor.http.encodeURLQueryComponent
import io.ktor.http.encodedPath
import io.ktor.http.takeFrom
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import me.him188.ani.client.auth.ApiKeyAuth
import me.him188.ani.client.auth.Authentication
import me.him188.ani.client.auth.HttpBasicAuth
import me.him188.ani.client.auth.HttpBearerAuth
import me.him188.ani.client.auth.OAuth

open class ApiClient(
        private val baseUrl: String
) {

    private lateinit var client: HttpClient

    constructor(
        baseUrl: String,
        httpClientEngine: HttpClientEngine?,
        httpClientConfig: ((HttpClientConfig<*>) -> Unit)? = null,
        jsonBlock: Json,
    ) : this(baseUrl = baseUrl) {
        val clientConfig: (HttpClientConfig<*>) -> Unit by lazy {
            {
                it.install(ContentNegotiation) { json(jsonBlock) }
                httpClientConfig?.invoke(it)
            }
        }

        client = httpClientEngine?.let { HttpClient(it, clientConfig) } ?: HttpClient(clientConfig)
    }

    constructor(
        baseUrl: String,
        httpClient: HttpClient
    ): this(baseUrl = baseUrl) {
        this.client = httpClient
    }

    private val authentications: kotlin.collections.Map<String, Authentication> by lazy {
        mapOf(
                "auth-jwt" to HttpBearerAuth("bearer"))
    }

    companion object {
        const val BASE_URL: String = "https://danmaku-global.myani.org"
        val JSON_DEFAULT: Json = Json {
          ignoreUnknownKeys = true
          prettyPrint = true
          isLenient = true
        }
        protected val UNSAFE_HEADERS: List<String> = listOf(HttpHeaders.ContentType)
    }

    /**
     * Set the username for the first HTTP basic authentication.
     *
     * @param username Username
     */
    fun setUsername(username: String) {
        val auth = authentications?.values?.firstOrNull { it is HttpBasicAuth } as HttpBasicAuth?
                ?: throw Exception("No HTTP basic authentication configured")
        auth.username = username
    }

    /**
     * Set the password for the first HTTP basic authentication.
     *
     * @param password Password
     */
    fun setPassword(password: String) {
        val auth = authentications?.values?.firstOrNull { it is HttpBasicAuth } as HttpBasicAuth?
                ?: throw Exception("No HTTP basic authentication configured")
        auth.password = password
    }

    /**
     * Set the API key value for the first API key authentication.
     *
     * @param apiKey API key
     * @param paramName The name of the API key parameter, or null or set the first key.
     */
    fun setApiKey(apiKey: String, paramName: String? = null) {
        val auth = authentications?.values?.firstOrNull { it is ApiKeyAuth && (paramName == null || paramName == it.paramName)} as ApiKeyAuth?
                ?: throw Exception("No API key authentication configured")
        auth.apiKey = apiKey
    }

    /**
     * Set the API key prefix for the first API key authentication.
     *
     * @param apiKeyPrefix API key prefix
     * @param paramName The name of the API key parameter, or null or set the first key.
     */
    fun setApiKeyPrefix(apiKeyPrefix: String, paramName: String? = null) {
        val auth = authentications?.values?.firstOrNull { it is ApiKeyAuth && (paramName == null || paramName == it.paramName) } as ApiKeyAuth?
                ?: throw Exception("No API key authentication configured")
        auth.apiKeyPrefix = apiKeyPrefix
    }

    /**
     * Set the access token for the first OAuth2 authentication.
     *
     * @param accessToken Access token
     */
    fun setAccessToken(accessToken: String) {
        val auth = authentications?.values?.firstOrNull { it is OAuth } as OAuth?
                ?: throw Exception("No OAuth2 authentication configured")
        auth.accessToken = accessToken
    }

    /**
     * Set the access token for the first Bearer authentication.
     *
     * @param bearerToken The bearer token.
     */
    fun setBearerToken(bearerToken: String) {
        val auth = authentications?.values?.firstOrNull { it is HttpBearerAuth } as HttpBearerAuth?
                ?: throw Exception("No Bearer authentication configured")
        auth.bearerToken = bearerToken
    }

    protected suspend fun <T: Any?> multipartFormRequest(requestConfig: RequestConfig<T>, body: kotlin.collections.List<PartData>?, authNames: kotlin.collections.List<String>): HttpResponse {
        return request(requestConfig, MultiPartFormDataContent(body ?: listOf()), authNames)
    }

    protected suspend fun <T: Any?> urlEncodedFormRequest(requestConfig: RequestConfig<T>, body: Parameters?, authNames: kotlin.collections.List<String>): HttpResponse {
        return request(requestConfig, FormDataContent(body ?: Parameters.Empty), authNames)
    }

    protected suspend fun <T: Any?> jsonRequest(requestConfig: RequestConfig<T>, body: Any? = null, authNames: kotlin.collections.List<String>): HttpResponse = request(requestConfig, body, authNames)

    protected suspend fun <T: Any?> request(requestConfig: RequestConfig<T>, body: Any? = null, authNames: kotlin.collections.List<String>): HttpResponse {
        requestConfig.updateForAuth<T>(authNames)
        val headers = requestConfig.headers

        return client.request {
            this.url {
                this.takeFrom(URLBuilder(baseUrl))
                appendPath(requestConfig.path.trimStart('/').split('/'))
                requestConfig.query.forEach { query ->
                    query.value.forEach { value ->
                        parameter(query.key, value)
                    }
                }
            }
            this.method = requestConfig.method.httpMethod
            headers.filter { header -> !UNSAFE_HEADERS.contains(header.key) }.forEach { header -> this.header(header.key, header.value) }
            if (requestConfig.method in listOf(RequestMethod.PUT, RequestMethod.POST, RequestMethod.PATCH)) {
                val contentType = (requestConfig.headers[HttpHeaders.ContentType]?.let { ContentType.parse(it) }
                    ?: ContentType.Application.Json)
                this.contentType(contentType)
                this.setBody(body)
            }
        }
    }

    private fun <T: Any?> RequestConfig<T>.updateForAuth(authNames: kotlin.collections.List<String>) {
        for (authName in authNames) {
            val auth = authentications?.get(authName) ?: throw Exception("Authentication undefined: $authName")
            auth.apply(query, headers)
        }
    }

    private fun URLBuilder.appendPath(components: kotlin.collections.List<String>): URLBuilder = apply {
        encodedPath = encodedPath.trimEnd('/') + components.joinToString("/", prefix = "/") { it.encodeURLQueryComponent() }
    }

    private val RequestMethod.httpMethod: HttpMethod
        get() = when (this) {
            RequestMethod.DELETE -> HttpMethod.Delete
            RequestMethod.GET -> HttpMethod.Get
            RequestMethod.HEAD -> HttpMethod.Head
            RequestMethod.PATCH -> HttpMethod.Patch
            RequestMethod.PUT -> HttpMethod.Put
            RequestMethod.POST -> HttpMethod.Post
            RequestMethod.OPTIONS -> HttpMethod.Options
        }
}

// @formatter:on
