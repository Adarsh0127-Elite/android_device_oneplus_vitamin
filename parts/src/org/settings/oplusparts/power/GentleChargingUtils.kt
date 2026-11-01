/*
 * Copyright (C) 2018-2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.settings.oplusparts.power

import android.content.Context
import androidx.preference.PreferenceManager
import org.settings.oplusparts.FileUtils
import org.settings.oplusparts.GENTLE_CHARGING_NODE
import org.settings.oplusparts.PREF_GENTLE_CHARGING_KEY
import org.settings.oplusparts.PREF_GENTLE_CHARGING_WATT_KEY

object GentleChargingUtils {

    const val DEFAULT_WATT_CAP = 15
    const val MIN_WATT_CAP = 10
    const val MAX_WATT_CAP = 30

    fun isSupported(): Boolean = FileUtils.fileExists(GENTLE_CHARGING_NODE)

    fun isEnabled(context: Context): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getBoolean(PREF_GENTLE_CHARGING_KEY, false)
    }

    fun getWattCap(context: Context): Int {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getInt(PREF_GENTLE_CHARGING_WATT_KEY, DEFAULT_WATT_CAP)
            .coerceIn(MIN_WATT_CAP, MAX_WATT_CAP)
    }

    fun getHardwareEnabled(): Boolean {
        val line = FileUtils.readOneLine(GENTLE_CHARGING_NODE)?.trim() ?: return false
        val parts = line.split(",")
        return if (parts.size >= 3) {
            parts[2].trim() == "1"
        } else {
            false
        }
    }

    fun setGentleCharging(context: Context, enabled: Boolean, watt: Int = getWattCap(context)): Boolean {
        val clampedWatt = watt.coerceIn(MIN_WATT_CAP, MAX_WATT_CAP)
        val cmd = if (enabled) {
            "50,$clampedWatt,1"
        } else {
            "100,65,0"
        }

        val success = FileUtils.writeLine(GENTLE_CHARGING_NODE, cmd)
        if (success) {
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            prefs.edit()
                .putBoolean(PREF_GENTLE_CHARGING_KEY, enabled)
                .putInt(PREF_GENTLE_CHARGING_WATT_KEY, clampedWatt)
                .apply()
            GentleChargingTileService.updateTile(context)
        }
        return success
    }

    fun restoreOnBoot(context: Context) {
        if (!isSupported()) return
        if (isEnabled(context)) {
            val watt = getWattCap(context)
            FileUtils.writeLine(GENTLE_CHARGING_NODE, "50,$watt,1")
        }
    }
}
