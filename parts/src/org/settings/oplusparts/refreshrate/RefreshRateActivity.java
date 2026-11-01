/*
 * SPDX-FileCopyrightText: 2025 AlphaDroid
 * SPDX-FileCopyrightText: 2026 Evolution X
 * SPDX-License-Identifier: Apache-2.0
 */

package org.settings.oplusparts.refreshrate;

import android.os.Bundle;

import com.android.settingslib.collapsingtoolbar.CollapsingToolbarBaseActivity;
import org.settings.oplusparts.R;

public class RefreshRateActivity extends CollapsingToolbarBaseActivity {

    private static final String TAG = "RefreshRateActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(getString(R.string.refresh_rate_title));
        getSupportFragmentManager().beginTransaction()
                .replace(
                        com.android.settingslib.collapsingtoolbar.R.id.content_frame,
                        new RefreshRateFragment(),
                        TAG
                )
                .commit();
    }
}
