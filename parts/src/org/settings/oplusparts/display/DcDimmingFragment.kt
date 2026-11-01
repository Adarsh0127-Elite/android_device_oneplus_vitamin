/*
 * Copyright (C) 2018-2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.settings.oplusparts.display

import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.Preference.OnPreferenceChangeListener
import com.android.settingslib.widget.MainSwitchPreference
import com.android.settingslib.widget.SettingsBasePreferenceFragment
import org.settings.oplusparts.PREF_DC_DIMMING_KEY
import org.settings.oplusparts.R

class DcDimmingFragment : SettingsBasePreferenceFragment(), OnPreferenceChangeListener {

    private lateinit var mainSwitch: MainSwitchPreference

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.dc_dimming_settings, rootKey)

        mainSwitch = findPreference(PREF_DC_DIMMING_KEY)!!
        mainSwitch.isChecked = DcDimmingUtils.isEnabled(requireContext())
        mainSwitch.onPreferenceChangeListener = this
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
        if (preference.key == PREF_DC_DIMMING_KEY) {
            val enabled = newValue as Boolean
            return DcDimmingUtils.setEnabled(requireContext(), enabled)
        }
        return false
    }
}
