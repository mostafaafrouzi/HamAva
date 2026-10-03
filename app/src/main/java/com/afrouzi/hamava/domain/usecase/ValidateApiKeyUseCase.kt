/*
 * Copyright (c) 2026 Mostafa Afrouzi
 * Licensed under the Apache License 2.0
 * https://github.com/mostafaafrouzi/HamAva
 */

package com.afrouzi.hamava.domain.usecase

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import javax.inject.Inject

sealed class ApiKeyValidationResult {
    object Valid : ApiKeyValidationResult()
    object Invalid : ApiKeyValidationResult()
    data class NetworkFailure(val error: String) : ApiKeyValidationResult()
}

class ValidateApiKeyUseCase @Inject constructor() {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend operator fun invoke(apiKey: String): ApiKeyValidationResult = withContext(Dispatchers.IO) {
        val trimmed = apiKey.trim()
        if (trimmed.isBlank()) {
            return@withContext ApiKeyValidationResult.Invalid
        }

        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models?key=$trimmed"
            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                when (response.code) {
                    200 -> ApiKeyValidationResult.Valid
                    400, 403 -> ApiKeyValidationResult.Invalid
                    else -> ApiKeyValidationResult.NetworkFailure("HTTP ${response.code}")
                }
            }
        } catch (e: Exception) {
            ApiKeyValidationResult.NetworkFailure(e.localizedMessage ?: "Connection error")
        }
    }
}
