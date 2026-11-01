/*
 * Copyright (C) 2018-2026 The LineageOS Project
 * Copyright (C) 2026 Evolution X
 * SPDX-License-Identifier: Apache-2.0
 */

package org.settings.oplusparts

// Top-level constants for backwards compatibility
const val PREF_DC_DIMMING_KEY = "dc_dimming_enable"
const val DC_DIMMING_NODE = "/sys/kernel/oplus_display/dimlayer_bl_en"

const val PREF_GENTLE_CHARGING_KEY = "gentle_charging_enable"
const val PREF_GENTLE_CHARGING_WATT_KEY = "gentle_charging_watt"
const val GENTLE_CHARGING_NODE = "/sys/class/oplus_chg/battery/slow_chg_en"

object Constants {
    const val DEBUG = true

    // DC Dimming
    const val PREF_DC_DIMMING_KEY = "dc_dimming_enable"
    const val DC_DIMMING_NODE = "/sys/kernel/oplus_display/dimlayer_bl_en"

    // Gentle Charging
    const val PREF_GENTLE_CHARGING_KEY = "gentle_charging_enable"
    const val PREF_GENTLE_CHARGING_WATT_KEY = "gentle_charging_watt"
    const val GENTLE_CHARGING_NODE = "/sys/class/oplus_chg/battery/slow_chg_en"

    // Pause Charging (formerly Bypass Charging)
    const val NODE_PAUSE_CHARGING = "/sys/class/oplus_chg/battery/mmi_charging_enable"
    const val KEY_PAUSE_CHARGING = "pause_charging_enable"
    const val KEY_PAUSE_CHARGING_TARGET = "pause_charging_target"
    const val KEY_PAUSE_CHARGING_APPS = "pause_charging_apps"

    const val PAUSE_OFF = 0
    const val PAUSE_WAITING = 1
    const val PAUSE_ON = 2

    const val PAUSE_TARGET_MIN = 1
    const val PAUSE_TARGET_MAX = 99
    const val PAUSE_TARGET_DEFAULT = 80

    // Refresh rate
    const val KEY_REFRESH_RATE_MODE = "refresh_rate_mode"
    const val KEY_REFRESH_RATE_APPS = "refresh_rate_apps"
    const val REFRESH_RATE_DEFAULT = 0

    // GameBar
    const val KEY_GAMEBAR_AUTO_APPS = "game_bar_auto_apps"
}
