/*
 * SPDX-FileCopyrightText: 2025 AlphaDroid
 * SPDX-FileCopyrightText: 2026 Evolution X
 * SPDX-License-Identifier: Apache-2.0
 */

package org.settings.oplusparts.refreshrate;

import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceGroup;

import org.settings.oplusparts.Constants;
import org.settings.oplusparts.R;
import org.settings.oplusparts.preferences.AppRefreshRatePreference;
import org.settings.oplusparts.utils.AppListManager;
import org.settings.oplusparts.utils.PackageListAdapter;
import org.settings.oplusparts.utils.PackageListAdapter.PackageItem;

import java.util.HashSet;
import java.util.Map;

public class RefreshRateFragment extends PreferenceFragmentCompat {

    private static final String TAG = "RefreshRateFragment";
    private static final String REFRESH_RATE_ADD_PACKAGES = "refresh_rate_add_packages";
    private static final String REFRESH_RATE_APPLICATIONS = "refresh_rate_applications";

    private RefreshRateController mController;
    private AppListManager mAppListManager;

    private ListPreference mGlobalRefreshRate;
    private PreferenceGroup mAppsPreList;
    private Preference mAddAppsPref;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.refresh_rate_preferences, rootKey);

        mController = RefreshRateController.getInstance(getContext());

        mAppListManager = new AppListManager(
                getContext(),
                Constants.KEY_REFRESH_RATE_APPS,
                this::onAppListChanged
        );

        mGlobalRefreshRate = findPreference(Constants.KEY_REFRESH_RATE_MODE);
        if (mGlobalRefreshRate != null) {
            int currentGlobal = mController.getGlobalRefreshRate();
            mGlobalRefreshRate.setValue(String.valueOf(currentGlobal));
            mGlobalRefreshRate.setOnPreferenceChangeListener((pref, newValue) -> {
                try {
                    int fps = Integer.parseInt((String) newValue);
                    mController.setGlobalRefreshRate(fps);
                    updateSummary();
                    return true;
                } catch (NumberFormatException e) {
                    Log.w(TAG, "Invalid FPS value", e);
                    return false;
                }
            });
            updateSummary();
        }

        mAppsPreList = findPreference(REFRESH_RATE_APPLICATIONS);
        if (mAppsPreList != null) {
            mAppsPreList.setOrderingAsAdded(false);
        }

        mAddAppsPref = findPreference(REFRESH_RATE_ADD_PACKAGES);
        if (mAddAppsPref != null) {
            mAddAppsPref.setOnPreferenceClickListener(pref -> {
                showAppSelectionDialog();
                return true;
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshAppListUI();
    }

    private void updateSummary() {
        if (mGlobalRefreshRate != null) {
            int fps = mController.getGlobalRefreshRate();
            mGlobalRefreshRate.setSummary(
                    AppRefreshRatePreference.getFormattedSummary(getContext(), fps));
        }
    }

    private void refreshAppListUI() {
        if (mAppsPreList == null) {
            return;
        }

        int initialCount = mAppsPreList.getPreferenceCount();
        for (int i = initialCount - 1; i >= 0; i--) {
            Preference pref = mAppsPreList.getPreference(i);
            if (pref instanceof AppRefreshRatePreference) {
                mAppsPreList.removePreference(pref);
            }
        }

        Map<String, Integer> overrides = mController.getAppOverridesMap();
        for (Map.Entry<String, Integer> entry : overrides.entrySet()) {
            String packageName = entry.getKey();
            int fps = entry.getValue();

            AppRefreshRatePreference appPref = new AppRefreshRatePreference(getContext());
            appPref.setAppInfo(packageName, fps);

            appPref.setOnRefreshRateChangeListener((pkg, newFps) -> {
                if (Constants.DEBUG) Log.i(TAG, "Rate changed: " + pkg + " -> " + newFps);
                mController.setAppRefreshRate(pkg, newFps);
            });

            appPref.setOnDeleteClickListener(pkg -> {
                if (Constants.DEBUG) Log.i(TAG, "Delete clicked: " + pkg);
                showDeleteConfirmation(pkg);
            });

            mAppsPreList.addPreference(appPref);
        }

        if (overrides.isEmpty() && mAddAppsPref != null) {
            mAddAppsPref.setSummary(R.string.no_app_overrides);
        } else if (mAddAppsPref != null) {
            mAddAppsPref.setSummary("");
        }
    }

    private void showDeleteConfirmation(String packageName) {
        new AlertDialog.Builder(requireActivity())
                .setTitle(R.string.delete)
                .setMessage(R.string.delete_message)
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    mController.removeAppRefreshRate(packageName);
                    refreshAppListUI();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showAppSelectionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        final Dialog dialog;
        final ListView list = new ListView(requireActivity());

        PackageListAdapter adapter = new PackageListAdapter(requireActivity());

        HashSet<String> excludedPackages = new HashSet<>(mController.getAppOverridesMap().keySet());
        excludedPackages.add(getContext().getPackageName());
        adapter.setExcludedPackages(excludedPackages);

        list.setAdapter(adapter);
        list.setDivider(null);

        builder.setTitle(R.string.add_app);
        builder.setView(list);
        dialog = builder.create();

        list.setOnItemClickListener((parent, view, position, id) -> {
            PackageItem info = (PackageItem) parent.getItemAtPosition(position);
            if (info != null) {
                int globalFps = mController.getGlobalRefreshRate();
                mController.setAppRefreshRate(info.packageName, globalFps);
                if (Constants.DEBUG) Log.i(TAG, "Added app: " + info.packageName + " with fps: " + globalFps);
                refreshAppListUI();
            }
            dialog.dismiss();
        });

        if (!requireActivity().isFinishing()) {
            dialog.show();
        }
    }

    private void onAppListChanged() {
        RefreshRateMonitorService.notifyStateChanged(getContext());
    }
}
