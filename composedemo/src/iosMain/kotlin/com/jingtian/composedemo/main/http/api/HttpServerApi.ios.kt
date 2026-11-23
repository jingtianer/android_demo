package com.jingtian.composedemo.main.http.api

import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.darwin.Darwin

actual fun platformKtorEngine(): HttpClientEngineFactory<HttpClientEngineConfig> = Darwin