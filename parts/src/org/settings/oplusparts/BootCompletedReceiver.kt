/*
 * Copyright (C) 2018-2026 The LineageOS Project
 * Copyright (C) 2026 Evolution X
 * SPDX-License-Identifier: Apache-2.0
 */

package org.settings.oplusparts

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import org.settings.oplusparts.display.DcDimmingUtils
import org.settings.oplusparts.gamebar.GameBarMonitorService
import org.settings.oplusparts.power.GentleChargingUtils
import org.settings.oplusparts.power.PauseChargingController
import org.settings.oplusparts.refreshrate.RefreshRateMonitorService

class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_BOOT_COMPLETED -> {
                if (Constants.DEBUG) {
                    Log.i(TAG, "Boot completed event received: ${intent.action}")
                }

                // Restore DC Dimming
                DcDimmingUtils.restoreOnBoot(context)

                // Restore Gentle Charging
                GentleChargingUtils.restoreOnBoot(context)

                // Restore Pause Charging hardware state (failsafe to "1" if not paused)
                PauseChargingController.getInstance(context).reconcileHardwareState()

                // Start background parts service
                try {
                    val serviceIntent = Intent(context, OnePlusPartsService::class.java)
                    context.startService(serviceIntent)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to start OnePlusPartsService on boot", e)
                }

                // Refresh rate monitoring
                RefreshRateMonitorService.notifyStateChanged(context)

                // GameBar monitoring
                GameBarMonitorService.notifyStateChanged(context)
            }
        }
    }

    companion object {
        private const val TAG = "BootCompletedReceiver"
    }
}
