/*
 * Copyright (C) 2018-2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.settings.oplusparts

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.settings.oplusparts.display.DcDimmingUtils
import org.settings.oplusparts.power.GentleChargingUtils

class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_BOOT_COMPLETED -> {
                DcDimmingUtils.restoreOnBoot(context)
                GentleChargingUtils.restoreOnBoot(context)
            }
        }
    }
}
