/*
 * Copyright (C) 2018-2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.settings.oplusparts.power

import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import com.android.settingslib.widget.MainSwitchPreference
import com.android.settingslib.widget.SettingsBasePreferenceFragment
import com.android.settingslib.widget.SliderPreference
import org.settings.oplusparts.PREF_GENTLE_CHARGING_KEY
import org.settings.oplusparts.R

private const val PREF_WATT_STATUS = "gentle_charging_watt_status"
private const val PREF_WATT_SLIDER = "gentle_charging_watt_slider"
private const val CATEGORY_TUNING = "gentle_charging_tuning_category"

class GentleChargingFragment : SettingsBasePreferenceFragment(),
    Preference.OnPreferenceChangeListener {

    private lateinit var mainSwitch: MainSwitchPreference
    private lateinit var wattStatusPref: Preference
    private lateinit var wattSliderPref: SliderPreference
    private lateinit var tuningCategory: PreferenceCategory

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.gentle_charging_settings, rootKey)

        mainSwitch = findPreference(PREF_GENTLE_CHARGING_KEY)!!
        wattStatusPref = findPreference(PREF_WATT_STATUS)!!
        wattSliderPref = findPreference(PREF_WATT_SLIDER)!!
        tuningCategory = findPreference(CATEGORY_TUNING)!!

        val isEnabled = GentleChargingUtils.isEnabled(requireContext())
        mainSwitch.isChecked = isEnabled
        mainSwitch.onPreferenceChangeListener = this

        wattSliderPref.min = GentleChargingUtils.MIN_WATT_CAP
        wattSliderPref.max = GentleChargingUtils.MAX_WATT_CAP
        wattSliderPref.sliderIncrement = 5
        wattSliderPref.setTickVisible(true)
        wattSliderPref.setHapticFeedbackMode(SliderPreference.HAPTIC_FEEDBACK_MODE_ON_TICKS)

        val currentWatt = GentleChargingUtils.getWattCap(requireContext())
        wattSliderPref.value = currentWatt
        wattSliderPref.onPreferenceChangeListener = this

        updateWattStatus(currentWatt)
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
        return when (preference.key) {
            PREF_GENTLE_CHARGING_KEY -> {
                val enabled = newValue as Boolean
                val success = GentleChargingUtils.setGentleCharging(
                    requireContext(),
                    enabled,
                    wattSliderPref.value
                )
                success
            }
            PREF_WATT_SLIDER -> {
                val watt = (newValue as Int).coerceIn(
                    GentleChargingUtils.MIN_WATT_CAP,
                    GentleChargingUtils.MAX_WATT_CAP
                )
                val success = GentleChargingUtils.setGentleCharging(
                    requireContext(),
                    mainSwitch.isChecked,
                    watt
                )
                if (success) {
                    updateWattStatus(watt)
                }
                success
            }
            else -> false
        }
    }

    private fun updateWattStatus(watt: Int) {
        wattStatusPref.summary = getString(R.string.gentle_charging_watt_format, watt)
    }
}
