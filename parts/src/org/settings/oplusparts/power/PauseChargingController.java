/*
 * SPDX-FileCopyrightText: 2025 AlphaDroid
 * SPDX-FileCopyrightText: 2026 Evolution X
 * SPDX-License-Identifier: Apache-2.0
 *
 * State machine for pause charging logic.
 * Controls /sys/class/oplus_chg/battery/mmi_charging_enable safely with
 * strict mutual exclusivity against Lineage Health charging control.
 */

package org.settings.oplusparts.power;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.provider.Settings;
import android.util.Log;

import androidx.preference.PreferenceManager;

import org.settings.oplusparts.Constants;
import org.settings.oplusparts.FileUtils;

import java.util.Objects;

public class PauseChargingController {

    private static final String TAG = "PauseChargingController";
    private static final String CHARGING_PAUSED = "0";
    private static final String CHARGING_ENABLED = "1";
    private static final String KEY_BATTERY_LEVEL = "current_battery_level";

    private int mBatteryLevel;
    private final Context mContext;
    private final Object mLock = new Object();

    private static PauseChargingController sInstance;

    private String mActiveApp = null;

    public static final class PauseState {
        public final int globalStatus;
        public final String activeApp;
        public final int targetLevel;
        public final int currentLevel;

        public PauseState(int globalStatus, String activeApp, int targetLevel, int currentLevel) {
            this.globalStatus = globalStatus;
            this.activeApp = activeApp;
            this.targetLevel = targetLevel;
            this.currentLevel = currentLevel;
        }

        @Override
        public String toString() {
            return "PauseState{" +
                    "global=" + globalStatus +
                    ", app=" + activeApp +
                    ", level=" + currentLevel + "/" + targetLevel +
                    "}";
        }
    }

    public static synchronized PauseChargingController getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new PauseChargingController(context.getApplicationContext());
        }
        return sInstance;
    }

    private PauseChargingController(Context context) {
        mContext = context.getApplicationContext();
        mBatteryLevel = getLevelFromIntent();
        if (isValidLevel(mBatteryLevel)) {
            saveCurrentBatteryLevel(mBatteryLevel);
        }
    }

    public static boolean isLineageChargingControlEnabled(Context context) {
        try {
            Class<?> clazz = Class.forName("lineageos.providers.LineageSettings$System");
            java.lang.reflect.Method getInt = clazz.getMethod("getInt",
                    android.content.ContentResolver.class, String.class, int.class);
            int val = (int) getInt.invoke(null, context.getContentResolver(), "charging_control_enabled", 0);
            if (val != 0) return true;
        } catch (Throwable ignored) {
        }
        try {
            if (Settings.System.getInt(
                    context.getContentResolver(),
                    "charging_control_enabled", 0) != 0) {
                return true;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private int getLevelFromIntent() {
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent intent = mContext.registerReceiver(null, filter);
        if (intent == null) {
            if (Constants.DEBUG) Log.w(TAG, "Battery intent null");
            return -1;
        }
        int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        return (level >= 0 && scale > 0) ? (int) ((level / (float) scale) * 100) : -1;
    }

    private int getPlugTypeFromIntent() {
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent intent = mContext.registerReceiver(null, filter);
        if (intent == null) return 0;
        return intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
    }

    public boolean isPlugged() {
        return getPlugTypeFromIntent() != 0;
    }

    private boolean isValidLevel(int level) {
        return level >= 0 && level <= 100;
    }

    public boolean enableHardwarePause() {
        if (isLineageChargingControlEnabled(mContext)) {
            Log.w(TAG, "Lineage charging control is active - refusing to pause charging");
            return false;
        }

        try {
            FileUtils.writeLine(Constants.NODE_PAUSE_CHARGING, CHARGING_PAUSED);
            String verify = FileUtils.readLine(Constants.NODE_PAUSE_CHARGING);
            if (!CHARGING_PAUSED.equals(verify)) {
                Log.e(TAG, "Hardware pause charging verification failed");
                return false;
            }
            if (Constants.DEBUG) Log.i(TAG, "Hardware charging paused (node=0)");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to pause hardware charging", e);
            return false;
        }
    }

    public boolean disableHardwarePause() {
        try {
            FileUtils.writeLine(Constants.NODE_PAUSE_CHARGING, CHARGING_ENABLED);
            String verify = FileUtils.readLine(Constants.NODE_PAUSE_CHARGING);
            if (!CHARGING_ENABLED.equals(verify)) {
                Log.e(TAG, "Hardware resume charging verification failed");
                return false;
            }
            if (Constants.DEBUG) Log.i(TAG, "Hardware charging resumed (node=1)");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to resume hardware charging", e);
            return false;
        }
    }

    public boolean isPauseChargingSupported() {
        return FileUtils.fileExists(Constants.NODE_PAUSE_CHARGING);
    }

    public void reconcileHardwareState() {
        synchronized (mLock) {
            if (isLineageChargingControlEnabled(mContext)) {
                if (Constants.DEBUG) Log.i(TAG, "Lineage charging control active during reconcile; ensuring safe node");
                disableHardwarePause();
                return;
            }

            int level = getLevelFromIntent();
            if (isValidLevel(level)) {
                mBatteryLevel = level;
                saveCurrentBatteryLevel(level);
            }

            int status = getPauseChargingStatus();
            int target = getPauseChargingTarget();
            int current = getCurrentBatteryLevel();
            boolean wantPause;

            if (status == Constants.PAUSE_OFF) {
                wantPause = (mActiveApp != null) && (current >= target);
            } else {
                wantPause = current >= target;
                if (wantPause && status == Constants.PAUSE_WAITING) {
                    savePauseChargingStatus(Constants.PAUSE_ON);
                } else if (!wantPause && status == Constants.PAUSE_ON) {
                    savePauseChargingStatus(Constants.PAUSE_WAITING);
                }
            }

            if (wantPause && isPlugged()) {
                enableHardwarePause();
            } else {
                disableHardwarePause();
            }

            if (Constants.DEBUG) Log.i(TAG, "Reconciled hardware state: " + getState());
            notifyStateChanged();
        }
    }

    public void handlePowerConnected() {
        if (Constants.DEBUG) Log.i(TAG, "Power connected");
        reconcileHardwareState();
    }

    public void handlePowerDisconnected() {
        synchronized (mLock) {
            if (Constants.DEBUG) Log.i(TAG, "Power disconnected - restoring charging to enabled");
            disableHardwarePause();
            mActiveApp = null;
            notifyStateChanged();
        }
    }

    public boolean enablePauseCharging() {
        synchronized (mLock) {
            if (isLineageChargingControlEnabled(mContext)) {
                Log.w(TAG, "Cannot enable pause charging: Lineage charging control active");
                return false;
            }

            int current = getCurrentBatteryLevel();
            if (!isValidLevel(current)) {
                Log.w(TAG, "Invalid battery level: " + current);
                return false;
            }

            mActiveApp = null;
            int target = getPauseChargingTarget();

            if (current >= target && isPlugged()) {
                if (enableHardwarePause()) {
                    savePauseChargingStatus(Constants.PAUSE_ON);
                    if (Constants.DEBUG) Log.i(TAG, "Global pause enabled immediately");
                    notifyStateChanged();
                    return true;
                }
                return false;
            } else {
                savePauseChargingStatus(Constants.PAUSE_WAITING);
                if (Constants.DEBUG) Log.i(TAG, "Global pause enabled, waiting for target " + target + "%");
                notifyStateChanged();
                return true;
            }
        }
    }

    public boolean disablePauseCharging() {
        synchronized (mLock) {
            disableHardwarePause();
            mActiveApp = null;
            savePauseChargingStatus(Constants.PAUSE_OFF);
            if (Constants.DEBUG) Log.i(TAG, "Global pause disabled");
            notifyStateChanged();
            return true;
        }
    }

    public void setActiveApp(String packageName) {
        synchronized (mLock) {
            if (getPauseChargingStatus() != Constants.PAUSE_OFF) {
                if (Constants.DEBUG) Log.w(TAG, "Cannot set app: global pause is active");
                return;
            }

            if (Objects.equals(mActiveApp, packageName)) {
                return;
            }

            mActiveApp = packageName;
            int current = getCurrentBatteryLevel();
            int target = getPauseChargingTarget();

            if (current >= target && isPlugged()) {
                enableHardwarePause();
                if (Constants.DEBUG) Log.i(TAG, "App pause enabled for " + packageName);
            } else {
                disableHardwarePause();
                if (Constants.DEBUG) Log.i(TAG, "App pause waiting for target for " + packageName);
            }

            notifyStateChanged();
        }
    }

    public void clearActiveApp() {
        synchronized (mLock) {
            if (mActiveApp == null) return;
            disableHardwarePause();
            mActiveApp = null;
            if (Constants.DEBUG) Log.i(TAG, "App pause cleared");
            notifyStateChanged();
        }
    }

    public void onBatteryLevelChanged(int level) {
        synchronized (mLock) {
            if (!isValidLevel(level)) return;

            if (mBatteryLevel != level) {
                mBatteryLevel = level;
                saveCurrentBatteryLevel(level);
            }

            if (isLineageChargingControlEnabled(mContext)) {
                disableHardwarePause();
                return;
            }

            int status = getPauseChargingStatus();
            int target = getPauseChargingTarget();

            if (status == Constants.PAUSE_WAITING && level >= target) {
                if (isPlugged() && enableHardwarePause()) {
                    savePauseChargingStatus(Constants.PAUSE_ON);
                    if (Constants.DEBUG) Log.i(TAG, "Global: reached target at " + level + "%");
                    notifyStateChanged();
                    return;
                }
            }

            if (status == Constants.PAUSE_ON && level < target) {
                disableHardwarePause();
                savePauseChargingStatus(Constants.PAUSE_WAITING);
                if (Constants.DEBUG) Log.i(TAG, "Global: dropped below target at " + level + "%, recharging");
                notifyStateChanged();
                return;
            }

            if (mActiveApp != null && status == Constants.PAUSE_OFF && level >= target) {
                if (isPlugged() && enableHardwarePause()) {
                    if (Constants.DEBUG) Log.i(TAG, "App: reached target at " + level + "%");
                    notifyStateChanged();
                    return;
                }
            }

            if (mActiveApp != null && status == Constants.PAUSE_OFF && level < target) {
                disableHardwarePause();
                if (Constants.DEBUG) Log.i(TAG, "App: dropped below target at " + level + "%, recharging");
                notifyStateChanged();
                return;
            }

            notifyStateChanged();
        }
    }

    public void setPauseChargingTarget(int target) {
        synchronized (mLock) {
            if (target < Constants.PAUSE_TARGET_MIN || target > Constants.PAUSE_TARGET_MAX) {
                Log.w(TAG, "Invalid target: " + target);
                return;
            }

            int old = getPauseChargingTarget();
            if (old == target) return;

            savePauseChargingTarget(target);
            int status = getPauseChargingStatus();
            int current = getCurrentBatteryLevel();

            if (status == Constants.PAUSE_ON && current < target) {
                disableHardwarePause();
                savePauseChargingStatus(Constants.PAUSE_WAITING);
                if (Constants.DEBUG) Log.i(TAG, "Target raised, now WAITING");
            } else if (status == Constants.PAUSE_WAITING && current >= target) {
                if (isPlugged() && enableHardwarePause()) {
                    savePauseChargingStatus(Constants.PAUSE_ON);
                    if (Constants.DEBUG) Log.i(TAG, "Target lowered, now ON");
                }
            }

            notifyStateChanged();
        }
    }

    public PauseState getState() {
        synchronized (mLock) {
            return new PauseState(
                    getPauseChargingStatus(),
                    mActiveApp,
                    getPauseChargingTarget(),
                    getCurrentBatteryLevel()
            );
        }
    }

    private void savePauseChargingStatus(int status) {
        PreferenceManager.getDefaultSharedPreferences(mContext)
                .edit()
                .putInt(Constants.KEY_PAUSE_CHARGING, status)
                .apply();
    }

    public int getPauseChargingStatus() {
        return PreferenceManager.getDefaultSharedPreferences(mContext)
                .getInt(Constants.KEY_PAUSE_CHARGING, Constants.PAUSE_OFF);
    }

    private void savePauseChargingTarget(int target) {
        PreferenceManager.getDefaultSharedPreferences(mContext)
                .edit()
                .putInt(Constants.KEY_PAUSE_CHARGING_TARGET, target)
                .apply();
    }

    public int getPauseChargingTarget() {
        int target = PreferenceManager.getDefaultSharedPreferences(mContext)
                .getInt(Constants.KEY_PAUSE_CHARGING_TARGET, Constants.PAUSE_TARGET_DEFAULT);
        if (target < Constants.PAUSE_TARGET_MIN || target > Constants.PAUSE_TARGET_MAX) {
            Log.w(TAG, "Invalid target: " + target);
            savePauseChargingTarget(Constants.PAUSE_TARGET_DEFAULT);
            return Constants.PAUSE_TARGET_DEFAULT;
        }
        return target;
    }

    private void saveCurrentBatteryLevel(int level) {
        if (isValidLevel(level)) {
            PreferenceManager.getDefaultSharedPreferences(mContext)
                    .edit()
                    .putInt(KEY_BATTERY_LEVEL, level)
                    .apply();
        }
    }

    public int getCurrentBatteryLevel() {
        int level = PreferenceManager.getDefaultSharedPreferences(mContext)
                .getInt(KEY_BATTERY_LEVEL, -1);
        if (!isValidLevel(level)) {
            level = getLevelFromIntent();
            if (isValidLevel(level)) {
                saveCurrentBatteryLevel(level);
            }
        }
        return level;
    }

    private void notifyStateChanged() {
        PauseState state = getState();
        PauseChargingManager.notifyStateChanged(mContext, state);
    }
}
