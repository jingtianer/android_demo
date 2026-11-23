package com.jingtian.composedemo.main.http.api

import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.apache5.Apache5

actual fun platformKtorEngine(): HttpClientEngineFactory<HttpClientEngineConfig> = Apache5
