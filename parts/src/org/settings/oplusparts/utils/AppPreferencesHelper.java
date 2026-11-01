/*
 * SPDX-FileCopyrightText: 2025 AlphaDroid
 * SPDX-FileCopyrightText: 2026 Evolution X
 * SPDX-License-Identifier: Apache-2.0
 */

package org.settings.oplusparts.utils;

import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;
import android.widget.ListView;

import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;

import org.settings.oplusparts.Constants;

import java.util.HashSet;
import java.util.Map;

public class AppPreferencesHelper {
    private static final String TAG = "AppPreferencesHelper";

    public interface OnAppPreferenceClickListener {
        void onAppClicked(String packageName);
    }

    public static void refreshAppPreferences(
            PreferenceGroup prefGroup,
            Preference addButtonPref,
            Map<String, ?> appList,
            Context context,
            OnAppPreferenceClickListener clickListener) {

        if (prefGroup == null) {
            return;
        }

        int initialCount = prefGroup.getPreferenceCount();
        for (int i = initialCount - 1; i >= 0; i--) {
            Preference pref = prefGroup.getPreference(i);
            if (!pref.equals(addButtonPref)) {
                prefGroup.removePreference(pref);
            }
        }

        PackageManager pm = context.getPackageManager();
        for (String packageName : appList.keySet()) {
            try {
                Preference appPref = new Preference(context);
                appPref.setKey(packageName);
                appPref.setTitle(pm.getApplicationInfo(packageName, 0).loadLabel(pm));
                appPref.setIcon(pm.getApplicationInfo(packageName, 0).loadIcon(pm));
                appPref.setPersistent(false);

                if (clickListener != null) {
                    appPref.setOnPreferenceClickListener(pref -> {
                        clickListener.onAppClicked(packageName);
                        return true;
                    });
                }

                prefGroup.addPreference(appPref);
            } catch (PackageManager.NameNotFoundException e) {
                if (Constants.DEBUG) {
                    Log.w(TAG, "App not found: " + packageName, e);
                }
            }
        }
    }

    public static void setupPackageListAdapter(
            ListView list,
            HashSet<String> excludedPackages,
            Context context) {

        PackageListAdapter adapter = new PackageListAdapter(context);
        adapter.setExcludedPackages(excludedPackages);
        list.setAdapter(adapter);
        list.setDivider(null);
    }
}
