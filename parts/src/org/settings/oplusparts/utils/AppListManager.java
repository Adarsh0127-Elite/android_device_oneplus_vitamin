/*
 * SPDX-FileCopyrightText: 2025 AlphaDroid
 * SPDX-FileCopyrightText: 2026 Evolution X
 * SPDX-License-Identifier: Apache-2.0
 */

package org.settings.oplusparts.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.text.TextUtils;
import android.util.Log;

import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceManager;

import org.settings.oplusparts.Constants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AppListManager {
    private static final String TAG = "AppListManager";

    private final Context mContext;
    private final String mPreferenceKey;
    private final PackageManager mPackageManager;
    private final Callback mCallback;

    private String mCachedPackageList;
    private final Map<String, AppItem> mAppMap;

    public interface Callback {
        void onAppListChanged();
    }

    public static class AppItem {
        public final String packageName;

        public AppItem(String packageName) {
            this.packageName = packageName;
        }

        @Override
        public String toString() {
            return packageName;
        }

        public static AppItem fromString(String value) {
            if (TextUtils.isEmpty(value)) return null;
            return new AppItem(value);
        }
    }

    public AppListManager(Context context, String preferenceKey, Callback callback) {
        this.mContext = context.getApplicationContext();
        this.mPreferenceKey = preferenceKey;
        this.mCallback = callback;
        this.mPackageManager = mContext.getPackageManager();
        this.mAppMap = new HashMap<>();
    }

    public boolean refreshAppList() {
        String appListString = PreferenceManager.getDefaultSharedPreferences(mContext)
                .getString(mPreferenceKey, "");

        if (!TextUtils.equals(mCachedPackageList, appListString)) {
            mCachedPackageList = appListString;
            mAppMap.clear();
            parseAndAddToMap(appListString, mAppMap);
            if (Constants.DEBUG) Log.i(TAG, "App list refreshed: " + mAppMap.size() + " apps");
            return true;
        }
        return false;
    }

    public Map<String, AppItem> getAppList() {
        return new HashMap<>(mAppMap);
    }

    public void addApp(String packageName) {
        AppItem app = mAppMap.get(packageName);
        if (app == null) {
            app = new AppItem(packageName);
            mAppMap.put(packageName, app);
            saveAppList();
            if (Constants.DEBUG) Log.i(TAG, "App added: " + packageName);
            notifyCallback();
        }
    }

    public void removeApp(String packageName) {
        if (mAppMap.remove(packageName) != null) {
            saveAppList();
            if (Constants.DEBUG) Log.i(TAG, "App removed: " + packageName);
            notifyCallback();
        }
    }

    public boolean containsApp(String packageName) {
        return mAppMap.containsKey(packageName);
    }

    public void refreshPreferences(PreferenceGroup prefGroup, Preference addAppPref,
                                   OnAppClickListener listener) {
        if (prefGroup != null) {
            prefGroup.removeAll();

            for (AppItem app : mAppMap.values()) {
                try {
                    Preference pref = createPreferenceFromInfo(app, listener);
                    prefGroup.addPreference(pref);
                } catch (PackageManager.NameNotFoundException e) {
                    if (Constants.DEBUG) Log.w(TAG, "App not found: " + app.packageName, e);
                }
            }

            if (addAppPref != null) {
                addAppPref.setOrder(0);
                prefGroup.addPreference(addAppPref);
            }
        }
    }

    private Preference createPreferenceFromInfo(AppItem app, OnAppClickListener listener)
            throws PackageManager.NameNotFoundException {
        PackageInfo info = mPackageManager.getPackageInfo(app.packageName,
                PackageManager.GET_META_DATA);

        Preference pref = new Preference(mContext);
        pref.setKey(app.packageName);
        pref.setTitle(info.applicationInfo.loadLabel(mPackageManager));
        pref.setIcon(info.applicationInfo.loadIcon(mPackageManager));
        pref.setPersistent(false);

        if (listener != null) {
            pref.setOnPreferenceClickListener(p -> {
                listener.onAppClicked(app.packageName);
                return true;
            });
        }

        return pref;
    }

    public List<AppItem> getAvailableApps(String currentPackageName) {
        List<AppItem> available = new ArrayList<>();
        List<PackageInfo> packages = mPackageManager.getInstalledPackages(
                PackageManager.GET_META_DATA);

        for (PackageInfo info : packages) {
            boolean isSystem = (info.applicationInfo.flags & android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0;
            boolean isSelf = info.packageName.equals(currentPackageName);
            boolean alreadyInList = mAppMap.containsKey(info.packageName);

            if (!isSystem && !isSelf && !alreadyInList) {
                available.add(new AppItem(info.packageName));
            }
        }

        if (Constants.DEBUG) Log.i(TAG, "Available apps: " + available.size());
        return available;
    }

    public interface OnAppClickListener {
        void onAppClicked(String packageName);
    }

    private void parseAndAddToMap(String baseString, Map<String, AppItem> map) {
        if (baseString == null) return;

        final String[] array = TextUtils.split(baseString, "\\|");
        for (String item : array) {
            if (TextUtils.isEmpty(item)) continue;
            AppItem app = AppItem.fromString(item);
            if (app != null) {
                map.put(app.packageName, app);
            }
        }
    }

    private void saveAppList() {
        List<String> settings = new ArrayList<>();
        for (AppItem app : mAppMap.values()) {
            settings.add(app.toString());
        }
        final String value = TextUtils.join("|", settings);

        PreferenceManager.getDefaultSharedPreferences(mContext)
                .edit()
                .putString(mPreferenceKey, value)
                .apply();

        if (Constants.DEBUG) Log.i(TAG, "App list saved: " + mAppMap.size() + " apps");
    }

    private void notifyCallback() {
        if (mCallback != null) {
            mCallback.onAppListChanged();
        }
    }
}
