/*
 * Copyright (C) 2018-2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.settings.oplusparts.display

import android.content.Context
import androidx.preference.PreferenceManager
import org.settings.oplusparts.DC_DIMMING_NODE
import org.settings.oplusparts.FileUtils
import org.settings.oplusparts.PREF_DC_DIMMING_KEY

object DcDimmingUtils {

    fun isSupported(): Boolean = FileUtils.fileExists(DC_DIMMING_NODE)

    fun isEnabled(context: Context): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getBoolean(PREF_DC_DIMMING_KEY, false)
    }

    fun isEnabledInHardware(): Boolean =
        FileUtils.getFileValueAsBoolean(DC_DIMMING_NODE, false)

    fun setEnabled(context: Context, enabled: Boolean): Boolean {
        val success = FileUtils.writeLine(DC_DIMMING_NODE, if (enabled) "1" else "0")
        if (success) {
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            prefs.edit().putBoolean(PREF_DC_DIMMING_KEY, enabled).apply()
            DcDimmingTileService.updateTile(context)
        }
        return success
    }

    fun restoreOnBoot(context: Context) {
        if (!isSupported()) return
        val enabled = isEnabled(context)
        FileUtils.writeLine(DC_DIMMING_NODE, if (enabled) "1" else "0")
    }
}
