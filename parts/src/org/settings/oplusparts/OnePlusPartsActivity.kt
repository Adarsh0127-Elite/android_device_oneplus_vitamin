/*
 * Copyright (C) 2018-2026 The LineageOS Project
 * Copyright (C) 2026 Evolution X
 * SPDX-License-Identifier: Apache-2.0
 */

package org.settings.oplusparts

import android.os.Bundle
import com.android.settingslib.collapsingtoolbar.CollapsingToolbarBaseActivity

class OnePlusPartsActivity : CollapsingToolbarBaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle(getString(R.string.oneplus_parts_title))

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(
                    com.android.settingslib.collapsingtoolbar.R.id.content_frame,
                    OnePlusPartsFragment(),
                    TAG
                )
                .commit()
        }
    }

    companion object {
        private const val TAG = "OnePlusPartsActivity"
    }
}
