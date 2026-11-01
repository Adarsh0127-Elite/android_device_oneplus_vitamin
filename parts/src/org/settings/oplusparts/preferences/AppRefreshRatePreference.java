/*
 * SPDX-FileCopyrightText: 2025 AlphaDroid
 * SPDX-FileCopyrightText: 2026 Evolution X
 * SPDX-License-Identifier: Apache-2.0
 */

package org.settings.oplusparts.preferences;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import org.settings.oplusparts.R;

public class AppRefreshRatePreference extends Preference {

    private String mPackageName;
    private int mCurrentFps;
    private OnDeleteClickListener mDeleteListener;
    private OnRefreshRateChangeListener mChangeListener;

    public interface OnDeleteClickListener {
        void onDeleteClick(String packageName);
    }

    public interface OnRefreshRateChangeListener {
        void onRefreshRateChange(String packageName, int fps);
    }

    public AppRefreshRatePreference(Context context) {
        super(context);
        init();
    }

    public AppRefreshRatePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AppRefreshRatePreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setLayoutResource(R.layout.preference_app_refresh_rate);
    }

    public void setAppInfo(String packageName, int fps) {
        Context context = getContext();
        mPackageName = packageName;
        mCurrentFps = fps;

        try {
            PackageManager pm = context.getPackageManager();
            ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
            setTitle(pm.getApplicationLabel(appInfo));

            Drawable icon = pm.getApplicationIcon(packageName);
            setIcon(icon);
        } catch (PackageManager.NameNotFoundException e) {
            setTitle(packageName);
        }

        setSummary(getFormattedSummary(context, fps));
        notifyChanged();
    }

    public void setOnDeleteClickListener(OnDeleteClickListener listener) {
        mDeleteListener = listener;
    }

    public void setOnRefreshRateChangeListener(OnRefreshRateChangeListener listener) {
        mChangeListener = listener;
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        ImageView deleteButton = (ImageView) holder.findViewById(R.id.app_refresh_rate_delete);
        holder.itemView.setOnClickListener(v -> showRefreshRateDialog());

        if (deleteButton != null) {
            deleteButton.setOnClickListener(v -> {
                if (mDeleteListener != null) {
                    mDeleteListener.onDeleteClick(mPackageName);
                }
            });
        }
    }

    private void showRefreshRateDialog() {
        Context context = getContext();
        String[] options = context.getResources().getStringArray(R.array.refresh_rate_entries);
        String[] valueStrings = context.getResources().getStringArray(R.array.refresh_rate_values);
        int[] values = new int[valueStrings.length];
        for (int i = 0; i < valueStrings.length; i++) {
            values[i] = Integer.parseInt(valueStrings[i]);
        }

        int checkedItem = 0;
        for (int i = 0; i < values.length; i++) {
            if (values[i] == mCurrentFps) {
                checkedItem = i;
                break;
            }
        }

        new AlertDialog.Builder(context)
                .setTitle(R.string.refresh_rate_app_title_dialog)
                .setSingleChoiceItems(options, checkedItem, (dialog, which) -> {
                    int selectedFps = values[which];
                    if (selectedFps != mCurrentFps) {
                        mCurrentFps = selectedFps;
                        setSummary(getFormattedSummary(context, selectedFps));
                        if (mChangeListener != null) {
                            mChangeListener.onRefreshRateChange(mPackageName, selectedFps);
                        }
                    }
                    dialog.dismiss();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    public String getPackageName() {
        return mPackageName;
    }

    public int getCurrentFps() {
        return mCurrentFps;
    }

    public static String getFormattedSummary(Context context, int refreshRate) {
        switch (refreshRate) {
            case 0:
                return context.getResources().getString(R.string.refresh_rate_auto);
            case 60:
                return context.getResources().getString(R.string.refresh_rate_60hz);
            case 90:
                return context.getResources().getString(R.string.refresh_rate_90hz);
            case 120:
                return context.getResources().getString(R.string.refresh_rate_120hz);
        }
        return "";
    }
}
