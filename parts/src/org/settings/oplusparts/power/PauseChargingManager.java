/*
 * SPDX-FileCopyrightText: 2025 AlphaDroid
 * SPDX-FileCopyrightText: 2026 Evolution X
 * SPDX-License-Identifier: Apache-2.0
 *
 * Background service that monitors pause charging state:
 * - Battery level changes
 * - Foreground apps for app-based pause
 * - Restores hardware node to enabled (1) on service stop
 */

package org.settings.oplusparts.power;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

import androidx.preference.PreferenceManager;

import org.settings.oplusparts.Constants;
import org.settings.oplusparts.utils.ForegroundAppDetector;

import java.util.HashSet;
import java.util.Set;

public class PauseChargingManager extends Service {

    private static final String TAG = "PauseChargingManager";

    private static volatile PauseChargingManager sInstance;

    private Handler mHandler;
    private ForegroundAppDetector mForegroundDetector;
    private boolean mAppMonitoringActive = false;

    private BroadcastReceiver mBatteryReceiver;

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;
        mHandler = new Handler();
        mForegroundDetector = ForegroundAppDetector.getInstance(this);
        if (Constants.DEBUG) Log.i(TAG, "Created");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterBatteryReceiver();
        stopAppMonitoring();
        // Fail-safe: Always ensure charging is enabled when service terminates
        PauseChargingController.getInstance(this).disableHardwarePause();
        sInstance = null;
        if (Constants.DEBUG) Log.i(TAG, "Destroyed");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mHandler.post(this::handleStateChanged);
        return START_STICKY;
    }

    public static void notifyStateChanged(Context context, PauseChargingController.PauseState state) {
        if (state == null) return;

        PauseChargingManager instance = sInstance;
        if (instance == null) {
            Intent serviceIntent = new Intent(context, PauseChargingManager.class);
            try {
                context.startService(serviceIntent);
                if (Constants.DEBUG) Log.i(TAG, "Service started");
            } catch (Exception e) {
                Log.e(TAG, "Failed to start service", e);
            }
            return;
        }

        if (Constants.DEBUG) Log.i(TAG, "State changed: " + state.toString());
        instance.mHandler.post(instance::handleStateChanged);
    }

    private void handleStateChanged() {
        PauseChargingController controller = PauseChargingController.getInstance(this);

        if (PauseChargingController.isLineageChargingControlEnabled(this)) {
            if (Constants.DEBUG) Log.i(TAG, "Lineage charging control active - disabling pause charging");
            controller.disableHardwarePause();
            unregisterBatteryReceiver();
            stopAppMonitoring();
            stopSelf();
            return;
        }

        PauseChargingController.PauseState state = controller.getState();

        if (Constants.DEBUG) Log.i(TAG, "State: " + (state != null ? state.toString() : "null"));

        if (state != null) {
            boolean globalActive = state.globalStatus != Constants.PAUSE_OFF;
            boolean hasAppList = hasPauseAppsInPrefs();

            if (!globalActive && !hasAppList) {
                if (Constants.DEBUG) Log.i(TAG, "Nothing to monitor - stopping service");
                unregisterBatteryReceiver();
                stopAppMonitoring();
                controller.disableHardwarePause();
                stopSelf();
                return;
            }
        }

        registerBatteryReceiverIfNeeded();

        if (state != null) {
            boolean globalActive = state.globalStatus != Constants.PAUSE_OFF;
            boolean hasAppList = hasPauseAppsInPrefs();
            boolean shouldMonitorApps = !globalActive && hasAppList;

            if (shouldMonitorApps && !mAppMonitoringActive) {
                if (Constants.DEBUG) Log.i(TAG, "Starting app monitoring");
                startAppMonitoring();
            } else if (!shouldMonitorApps && mAppMonitoringActive) {
                if (Constants.DEBUG) Log.i(TAG, "Stopping app monitoring");
                stopAppMonitoring();
            }
        }
    }

    private void startAppMonitoring() {
        if (mAppMonitoringActive) {
            if (Constants.DEBUG) Log.w(TAG, "App monitoring already active");
            return;
        }

        mAppMonitoringActive = true;
        PauseChargingManager self = this;
        mForegroundDetector.startMonitoring("PauseCharging", packageName -> {
            if (Constants.DEBUG) Log.i(TAG, "Foreground app: " + packageName);

            PauseChargingController controller = PauseChargingController.getInstance(self);
            Set<String> pauseApps = self.getPauseAppsFromPrefs();

            if (pauseApps.contains(packageName)) {
                if (Constants.DEBUG) Log.i(TAG, "App in pause list - activating");
                controller.setActiveApp(packageName);
            } else {
                if (Constants.DEBUG) Log.i(TAG, "App not in pause list - clearing");
                controller.clearActiveApp();
            }
        });
        if (Constants.DEBUG) Log.i(TAG, "App monitoring started");
    }

    private void stopAppMonitoring() {
        if (!mAppMonitoringActive) return;

        mAppMonitoringActive = false;
        mForegroundDetector.stopMonitoring("PauseCharging");
        if (Constants.DEBUG) Log.i(TAG, "App monitoring stopped");
    }

    private Set<String> getPauseAppsFromPrefs() {
        String raw = PreferenceManager.getDefaultSharedPreferences(this)
                .getString(Constants.KEY_PAUSE_CHARGING_APPS, "");
        Set<String> apps = new HashSet<>();
        if (!TextUtils.isEmpty(raw)) {
            String[] packages = TextUtils.split(raw, "\\|");
            for (String pkg : packages) {
                if (!TextUtils.isEmpty(pkg)) {
                    apps.add(pkg.trim());
                }
            }
        }
        return apps;
    }

    private boolean hasPauseAppsInPrefs() {
        String raw = PreferenceManager.getDefaultSharedPreferences(this)
                .getString(Constants.KEY_PAUSE_CHARGING_APPS, "");
        return !TextUtils.isEmpty(raw);
    }

    private void registerBatteryReceiverIfNeeded() {
        if (mBatteryReceiver != null) return;

        mBatteryReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent == null) return;

                int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                if (level < 0 || scale <= 0) return;

                int pct = (int) ((level / (float) scale) * 100);

                PauseChargingController controller =
                        PauseChargingController.getInstance(getApplicationContext());
                controller.onBatteryLevelChanged(pct);
            }
        };

        try {
            registerReceiver(mBatteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (Constants.DEBUG) Log.i(TAG, "Battery receiver registered");
        } catch (Exception e) {
            Log.e(TAG, "Failed to register battery receiver", e);
            mBatteryReceiver = null;
        }
    }

    private void unregisterBatteryReceiver() {
        if (mBatteryReceiver != null) {
            try {
                unregisterReceiver(mBatteryReceiver);
            } catch (Exception ignored) {}
            mBatteryReceiver = null;
        }
    }
}
