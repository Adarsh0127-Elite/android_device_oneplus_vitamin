/*
 * Copyright (C) 2018-2026 The LineageOS Project
 * Copyright (C) 2026 Evolution X
 * SPDX-License-Identifier: Apache-2.0
 */

package org.settings.oplusparts

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat

class OnePlusPartsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.oneplus_parts_settings, rootKey)

        setupAlertSliderPreference()
    }

    private fun setupAlertSliderPreference() {
        val sliderPref = findPreference<Preference>("pref_alert_slider") ?: return
        val sliderCategory = findPreference<PreferenceCategory>("category_alert_slider")
        val pm = requireContext().packageManager

        // Check if ButtonSettingsActivity resolves on the device
        val actionIntent = Intent("org.lineageos.settings.device.ADDITIONAL_BUTTONS_SETTINGS").apply {
            setPackage("org.lineageos.settings.device")
        }
        val explicitIntent = Intent().apply {
            component = ComponentName(
                "org.lineageos.settings.device",
                "org.lineageos.settings.device.ButtonSettingsActivity"
            )
        }

        when {
            pm.queryIntentActivities(actionIntent, 0).isNotEmpty() -> {
                sliderPref.intent = actionIntent
                sliderPref.isVisible = true
            }
            pm.queryIntentActivities(explicitIntent, 0).isNotEmpty() -> {
                sliderPref.intent = explicitIntent
                sliderPref.isVisible = true
            }
            else -> {
                // Not resolved on device: hide both preference and category as instructed
                sliderPref.isVisible = false
                sliderCategory?.isVisible = false
            }
        }
    }
}
