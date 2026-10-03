/*
 * Copyright (c) 2026 Mostafa Afrouzi
 * Licensed under the Apache License 2.0
 * https://github.com/mostafaafrouzi/HamAva
 */

package com.afrouzi.hamava.domain.usecase

import android.content.Context
import android.content.Intent
import com.afrouzi.hamava.service.DubForegroundService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class StopDubbingUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {

    operator fun invoke() {
        val intent = Intent(context, DubForegroundService::class.java).apply {
            action = DubForegroundService.ACTION_STOP
        }
        try {
            context.startService(intent)
        } catch (e: Exception) {
            // Service might already be stopped
        }
    }
}
