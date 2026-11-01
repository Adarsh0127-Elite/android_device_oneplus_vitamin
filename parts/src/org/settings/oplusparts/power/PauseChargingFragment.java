/*
 * SPDX-FileCopyrightText: 2025 AlphaDroid
 * SPDX-FileCopyrightText: 2026 Evolution X
 * SPDX-License-Identifier: Apache-2.0
 */

package org.settings.oplusparts.power;

import android.app.Dialog;
import android.os.Bundle;
import android.widget.ListView;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceGroup;
import androidx.preference.TwoStatePreference;

import org.settings.oplusparts.Constants;
import org.settings.oplusparts.R;
import org.settings.oplusparts.preferences.CustomSeekBarPreference;
import org.settings.oplusparts.utils.AppListManager;
import org.settings.oplusparts.utils.AppPreferencesHelper;
import org.settings.oplusparts.utils.PackageListAdapter.PackageItem;

import java.util.HashSet;

public class PauseChargingFragment extends PreferenceFragmentCompat {

    private static final String PAUSE_CHARGING_ADD_PACKAGES = "pause_charging_add_packages";
    private static final String PAUSE_CHARGING_APPLICATIONS = "pause_charging_applications";

    private PreferenceGroup mPackagesPreList;
    private Preference mPackagesPref;

    private AppListManager mAppListManager;
    private TwoStatePreference mPausePreference;
    private CustomSeekBarPreference mTargetPreference;
    private PauseChargingController mPauseController;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.pause_charging_settings, rootKey);

        mPauseController = PauseChargingController.getInstance(getContext());

        mAppListManager = new AppListManager(
                getContext(),
                Constants.KEY_PAUSE_CHARGING_APPS,
                this::onAppListChanged
        );

        mPausePreference = findPreference(Constants.KEY_PAUSE_CHARGING);
        if (mPausePreference != null) {
            mPausePreference.setOnPreferenceChangeListener((pref, newValue) -> {
                boolean enable = (boolean) newValue;
                if (enable) {
                    if (PauseChargingController.isLineageChargingControlEnabled(getContext())) {
                        return false;
                    }
                    mPauseController.enablePauseCharging();
                } else {
                    mPauseController.disablePauseCharging();
                }
                return true;
            });
        }

        mTargetPreference = findPreference(Constants.KEY_PAUSE_CHARGING_TARGET);
        if (mTargetPreference != null) {
            mTargetPreference.setValue(mPauseController.getPauseChargingTarget());
            mTargetPreference.setMin(Constants.PAUSE_TARGET_MIN);
            mTargetPreference.setMax(Constants.PAUSE_TARGET_MAX);
            mTargetPreference.setDefaultValue(Constants.PAUSE_TARGET_DEFAULT, true);
            mTargetPreference.setOnPreferenceChangeListener((pref, newValue) -> {
                int target = (int) newValue;
                if (target < Constants.PAUSE_TARGET_MIN || target > Constants.PAUSE_TARGET_MAX) {
                    return false;
                }
                mPauseController.setPauseChargingTarget(target);
                return true;
            });
        }

        mPackagesPreList = findPreference(PAUSE_CHARGING_APPLICATIONS);
        if (mPackagesPreList != null) {
            mPackagesPreList.setOrderingAsAdded(false);
        }

        mPackagesPref = findPreference(PAUSE_CHARGING_ADD_PACKAGES);
        if (mPackagesPref != null) {
            mPackagesPref.setOnPreferenceClickListener(pref -> {
                showAppSelectionDialog();
                return true;
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mAppListManager.refreshAppList()) {
            refreshUI();
        }
        updatePauseToggleState();
    }

    private void updatePauseToggleState() {
        if (mPausePreference == null || mPauseController == null) return;

        boolean lineageActive = PauseChargingController.isLineageChargingControlEnabled(getContext());
        boolean supported = mPauseController.isPauseChargingSupported();

        if (lineageActive) {
            mPausePreference.setEnabled(false);
            mPausePreference.setChecked(false);
            mPausePreference.setSummary(R.string.pause_charging_disabled_by_lineage_health);
            if (mTargetPreference != null) mTargetPreference.setEnabled(false);
            if (mPackagesPref != null) mPackagesPref.setEnabled(false);
        } else if (!supported) {
            mPausePreference.setEnabled(false);
            mPausePreference.setChecked(false);
            mPausePreference.setSummary(R.string.pause_charging_unavailable);
            if (mTargetPreference != null) mTargetPreference.setEnabled(false);
            if (mPackagesPref != null) mPackagesPref.setEnabled(false);
        } else {
            mPausePreference.setEnabled(true);
            mPausePreference.setSummary(R.string.pause_charging_summary);
            boolean enabled = mPauseController.getPauseChargingStatus() != Constants.PAUSE_OFF;
            mPausePreference.setChecked(enabled);
            if (mTargetPreference != null) mTargetPreference.setEnabled(true);
            if (mPackagesPref != null) mPackagesPref.setEnabled(true);
        }
    }

    private void refreshUI() {
        AppPreferencesHelper.refreshAppPreferences(
                mPackagesPreList,
                mPackagesPref,
                mAppListManager.getAppList(),
                getContext(),
                this::showDeleteConfirmation
        );
    }

    private void showDeleteConfirmation(String packageName) {
        new AlertDialog.Builder(requireActivity())
                .setTitle(R.string.delete)
                .setMessage(R.string.delete_message)
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    mAppListManager.removeApp(packageName);
                    refreshUI();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showAppSelectionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        final Dialog dialog;
        final ListView list = new ListView(requireActivity());

        HashSet<String> excludedPackages = new HashSet<>(mAppListManager.getAppList().keySet());
        excludedPackages.add(getContext().getPackageName());

        AppPreferencesHelper.setupPackageListAdapter(list, excludedPackages, getContext());

        builder.setTitle(R.string.add_app);
        builder.setView(list);
        dialog = builder.create();

        list.setOnItemClickListener((parent, view, position, id) -> {
            PackageItem info = (PackageItem) parent.getItemAtPosition(position);
            mAppListManager.addApp(info.packageName);
            refreshUI();
            dialog.dismiss();
        });

        if (!requireActivity().isFinishing()) {
            dialog.show();
        }
    }

    private void onAppListChanged() {
        PauseChargingManager.notifyStateChanged(getContext(), mPauseController.getState());
    }
}
