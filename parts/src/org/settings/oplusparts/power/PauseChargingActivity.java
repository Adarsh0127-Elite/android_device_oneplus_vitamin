/*
 * SPDX-FileCopyrightText: 2025 AlphaDroid
 * SPDX-FileCopyrightText: 2026 Evolution X
 * SPDX-License-Identifier: Apache-2.0
 */

package org.settings.oplusparts.power;

import android.os.Bundle;

import com.android.settingslib.collapsingtoolbar.CollapsingToolbarBaseActivity;
import org.settings.oplusparts.R;

public class PauseChargingActivity extends CollapsingToolbarBaseActivity {

    private static final String TAG = "PauseChargingActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(getString(R.string.pause_charging_title));
        getSupportFragmentManager().beginTransaction()
                .replace(
                        com.android.settingslib.collapsingtoolbar.R.id.content_frame,
                        new PauseChargingFragment(),
                        TAG
                )
                .commit();
    }
}
