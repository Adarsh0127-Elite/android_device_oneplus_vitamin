/*
 * Copyright (C) 2018-2026 The LineageOS Project
 * Copyright (C) 2026 Evolution X
 * SPDX-License-Identifier: Apache-2.0
 *
 * Central service for OnePlusParts:
 * - Listens for power connect/disconnect events for pause charging fail-safe
 * - Initializes and reconciles hardware states on boot
 */

package org.settings.oplusparts;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import org.settings.oplusparts.gamebar.GameBarMonitorService;
import org.settings.oplusparts.power.PauseChargingController;
import org.settings.oplusparts.power.PauseChargingManager;
import org.settings.oplusparts.refreshrate.RefreshRateMonitorService;

public class OnePlusPartsService extends Service {
    private static final String TAG = "OnePlusPartsService";

    private BroadcastReceiver mPowerReceiver;

    @Override
    public void onCreate() {
        super.onCreate();
        if (Constants.DEBUG) Log.i(TAG, "Service created");

        initializeSubsystems();
        registerPowerReceiver();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (Constants.DEBUG) Log.i(TAG, "Service started");
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (Constants.DEBUG) Log.i(TAG, "Service destroyed");
        unregisterPowerReceiver();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void initializeSubsystems() {
        // Pause Charging: reconcile hardware state & launch manager if needed
        try {
            PauseChargingController pauseController = PauseChargingController.getInstance(this);
            pauseController.reconcileHardwareState();
            PauseChargingManager.notifyStateChanged(this, pauseController.getState());
        } catch (Exception e) {
            Log.e(TAG, "Failed initializing pause charging", e);
        }

        // Refresh rate
        try {
            RefreshRateMonitorService.notifyStateChanged(this);
        } catch (Exception e) {
            Log.e(TAG, "Failed initializing refresh rate service", e);
        }

        // GameBar
        try {
            GameBarMonitorService.notifyStateChanged(this);
        } catch (Exception e) {
            Log.e(TAG, "Failed initializing gamebar service", e);
        }
    }

    private void registerPowerReceiver() {
        if (mPowerReceiver != null) return;

        mPowerReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent == null) return;
                String action = intent.getAction();
                if (Intent.ACTION_POWER_CONNECTED.equals(action)) {
                    PauseChargingController.getInstance(context).handlePowerConnected();
                } else if (Intent.ACTION_POWER_DISCONNECTED.equals(action)) {
                    PauseChargingController.getInstance(context).handlePowerDisconnected();
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_POWER_CONNECTED);
        filter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        registerReceiver(mPowerReceiver, filter);
    }

    private void unregisterPowerReceiver() {
        if (mPowerReceiver != null) {
            try {
                unregisterReceiver(mPowerReceiver);
            } catch (Exception ignored) {
            }
            mPowerReceiver = null;
        }
    }
}
