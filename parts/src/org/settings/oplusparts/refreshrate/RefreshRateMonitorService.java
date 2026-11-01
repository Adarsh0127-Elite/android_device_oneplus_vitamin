/*
 * SPDX-FileCopyrightText: 2025 AlphaDroid
 * SPDX-FileCopyrightText: 2026 Evolution X
 * SPDX-License-Identifier: Apache-2.0
 *
 * Background service that monitors and applies refresh rate changes.
 * Uses Settings.System MIN/PEAK_REFRESH_RATE for clean framework integration.
 */

package org.settings.oplusparts.refreshrate;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;

import org.settings.oplusparts.Constants;
import org.settings.oplusparts.utils.ForegroundAppDetector;

public class RefreshRateMonitorService extends Service {

    private static final String TAG = "RefreshRateMonitorService";

    private static volatile RefreshRateMonitorService sInstance;

    private RefreshRateController mRefreshRateController;
    private Handler mHandler;
    private ForegroundAppDetector mForegroundDetector;
    private boolean mAppMonitoringActive = false;

    private static final float AUTO_MIN_REFRESH_RATE = 60f;
    private static final float AUTO_PEAK_REFRESH_RATE = 120f;

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;
        mRefreshRateController = RefreshRateController.getInstance(this);
        mHandler = new Handler();
        mForegroundDetector = ForegroundAppDetector.getInstance(this);
        if (Constants.DEBUG) Log.i(TAG, "Service created");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopAppMonitoring();
        sInstance = null;
        if (Constants.DEBUG) Log.i(TAG, "Service destroyed");
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

    public static void notifyStateChanged(Context context) {
        RefreshRateMonitorService instance = sInstance;
        if (instance == null) {
            Intent serviceIntent = new Intent(context, RefreshRateMonitorService.class);
            try {
                context.startService(serviceIntent);
                if (Constants.DEBUG) Log.i(TAG, "Service started");
            } catch (Exception e) {
                Log.e(TAG, "Failed to start service", e);
            }
            return;
        }

        if (Constants.DEBUG) Log.i(TAG, "State changed notification received");
        instance.mHandler.post(instance::handleStateChanged);
    }

    private void handleStateChanged() {
        RefreshRateController.RefreshRateState state = mRefreshRateController.getState();

        if (Constants.DEBUG) Log.i(TAG, "State: " + state.toString());

        boolean hasAppOverrides = mRefreshRateController.hasAppOverrides();

        if (!hasAppOverrides) {
            if (Constants.DEBUG) Log.i(TAG, "No app overrides - stopping app monitoring");
            stopAppMonitoring();
            applyRefreshRate(state.globalRefreshRate);
            return;
        }

        if (!mAppMonitoringActive) {
            if (Constants.DEBUG) Log.i(TAG, "Starting app monitoring");
            startAppMonitoring();
        } else {
            String pkg = mForegroundDetector.getCurrentForegroundApp();
            int targetFps = mRefreshRateController.getEffectiveRefreshRate(pkg);
            if (Constants.DEBUG) Log.i(TAG, "Applying effective rate " + targetFps
                    + " for current app " + pkg);
            applyRefreshRate(targetFps);
        }
    }

    private void startAppMonitoring() {
        if (mAppMonitoringActive) {
            if (Constants.DEBUG) Log.w(TAG, "App monitoring already active");
            return;
        }

        mAppMonitoringActive = true;

        mForegroundDetector.startMonitoring("RefreshRate", packageName -> {
            if (Constants.DEBUG) Log.i(TAG, "Foreground app: " + packageName);

            int targetFps = mRefreshRateController.getEffectiveRefreshRate(packageName);

            if (Constants.DEBUG) {
                String desc = targetFps == 0 ? "auto" : targetFps + " fps";
                Log.i(TAG, "Applying refresh rate: " + desc + " for " + packageName);
            }
            applyRefreshRate(targetFps);
        });

        if (Constants.DEBUG) Log.i(TAG, "App monitoring listener registered");
    }

    private void stopAppMonitoring() {
        if (!mAppMonitoringActive) {
            return;
        }

        mAppMonitoringActive = false;
        mForegroundDetector.stopMonitoring("RefreshRate");
        if (Constants.DEBUG) Log.i(TAG, "App monitoring stopped");
    }

    private void applyRefreshRate(int fps) {
        final float minRate = (fps == 0) ? AUTO_MIN_REFRESH_RATE : (float) fps;
        final float peakRate = (fps == 0) ? AUTO_PEAK_REFRESH_RATE : (float) fps;

        if (Constants.DEBUG) Log.i(TAG, "Applying MIN/PEAK refresh rate: "
                + minRate + "/" + peakRate + " Hz (fps=" + fps + ")");

        try {
            Settings.System.putFloatForUser(
                    getContentResolver(),
                    Settings.System.MIN_REFRESH_RATE,
                    minRate,
                    UserHandle.USER_CURRENT
            );

            Settings.System.putFloatForUser(
                    getContentResolver(),
                    Settings.System.PEAK_REFRESH_RATE,
                    peakRate,
                    UserHandle.USER_CURRENT
            );
        } catch (Exception e) {
            Log.e(TAG, "Failed to set refresh rate in Settings.System", e);
        }
    }
}
