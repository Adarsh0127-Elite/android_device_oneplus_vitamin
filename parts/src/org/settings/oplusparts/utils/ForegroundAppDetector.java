/*
 * SPDX-FileCopyrightText: 2025 AlphaDroid
 * SPDX-FileCopyrightText: 2026 Evolution X
 * SPDX-License-Identifier: Apache-2.0
 *
 * Shared foreground app detector using event-driven TaskStackListener.
 * Supports multiple concurrent listeners (GameBar, RefreshRate, and PauseCharging).
 */

package org.settings.oplusparts.utils;

import android.app.ActivityManager;
import android.app.ActivityTaskManager;
import android.app.TaskStackListener;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;

import org.settings.oplusparts.Constants;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ForegroundAppDetector {
    private static final String TAG = "ForegroundAppDetector";

    private static ForegroundAppDetector sInstance;
    private final Context mContext;
    private final Handler mHandler;

    private TaskStackListener mTaskStackListener;
    private BroadcastReceiver mScreenReceiver;
    private final List<ListenerWrapper> mListeners = new ArrayList<>();
    private final Object mListenerLock = new Object();

    private static final long DEBOUNCE_MS = 500L;
    private static final long FLOOD_GUARD_MS = 200L;
    private Runnable mDebouncedCallback;
    private long mLastEventTime = 0;
    private String mLastReportedApp = null;

    public interface ForegroundAppListener {
        void onForegroundAppChanged(String packageName);
    }

    private static class ListenerWrapper {
        final String id;
        final ForegroundAppListener listener;

        ListenerWrapper(String id, ForegroundAppListener listener) {
            this.id = id;
            this.listener = listener;
        }
    }

    public static synchronized ForegroundAppDetector getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new ForegroundAppDetector(context.getApplicationContext());
        }
        return sInstance;
    }

    private ForegroundAppDetector(Context context) {
        mContext = context;
        mHandler = new Handler(Looper.getMainLooper());
    }

    public void startMonitoring(String listenerId, ForegroundAppListener listener) {
        synchronized (mListenerLock) {
            for (ListenerWrapper w : mListeners) {
                if (w.id.equals(listenerId)) {
                    if (Constants.DEBUG) Log.w(TAG, "Listener already registered: " + listenerId);
                    return;
                }
            }
            mListeners.add(new ListenerWrapper(listenerId, listener));
            if (Constants.DEBUG) Log.i(TAG, "Listener registered: " + listenerId + " (total: " + mListeners.size() + ")");
        }

        if (mListeners.size() == 1) {
            registerTaskStackListenerIfNeeded();
            registerScreenReceiverIfNeeded();
            mHandler.post(this::handlePossibleTopAppChange);
        } else {
            mHandler.post(() -> {
                String topApp = getTopPackageName();
                if (!TextUtils.isEmpty(topApp)) {
                    try {
                        if (Constants.DEBUG) Log.i(TAG, "Initial report to " + listenerId + ": " + topApp);
                        listener.onForegroundAppChanged(topApp);
                    } catch (Exception e) {
                        Log.e(TAG, "Listener exception (" + listenerId + ")", e);
                    }
                }
            });
        }
    }

    public void stopMonitoring(String listenerId) {
        synchronized (mListenerLock) {
            mListeners.removeIf(w -> w.id.equals(listenerId));
            if (Constants.DEBUG) Log.i(TAG, "Listener unregistered: " + listenerId + " (remaining: " + mListeners.size() + ")");

            if (mListeners.isEmpty()) {
                unregisterTaskStackListener();
                unregisterScreenReceiver();
                cancelPendingCallback();
            }
        }
    }

    public String getCurrentForegroundApp() {
        return getTopPackageName();
    }

    private void handlePossibleTopAppChange() {
        String topApp = getTopPackageName();
        if (TextUtils.isEmpty(topApp)) {
            if (Constants.DEBUG) Log.i(TAG, "Top app is null/empty");
            return;
        }

        if (Objects.equals(topApp, mLastReportedApp)) {
            if (Constants.DEBUG) Log.i(TAG, "Same app, skipping: " + topApp);
            return;
        }

        if (Constants.DEBUG) Log.i(TAG, "Top app detected: " + topApp);

        long now = SystemClock.uptimeMillis();
        if (now - mLastEventTime < FLOOD_GUARD_MS) {
            if (Constants.DEBUG) Log.i(TAG, "Flood guard active for new app, will debounce");
        }
        mLastEventTime = now;

        cancelPendingCallback();

        final String scheduledApp = topApp;
        mDebouncedCallback = () -> {
            String currentTopApp = getTopPackageName();
            if (TextUtils.isEmpty(currentTopApp)) {
                if (Constants.DEBUG) Log.i(TAG, "Debounce: top app is null");
                return;
            }

            if (!Objects.equals(currentTopApp, scheduledApp)) {
                if (Constants.DEBUG) Log.i(TAG, "Debounce: app changed to " + currentTopApp);
                return;
            }

            mLastReportedApp = currentTopApp;

            List<ListenerWrapper> listeners;
            synchronized (mListenerLock) {
                listeners = new ArrayList<>(mListeners);
            }

            if (listeners.isEmpty()) {
                if (Constants.DEBUG) Log.w(TAG, "No listeners registered!");
                return;
            }

            for (ListenerWrapper w : listeners) {
                try {
                    if (Constants.DEBUG) Log.i(TAG, "Reporting to " + w.id + ": " + currentTopApp);
                    w.listener.onForegroundAppChanged(currentTopApp);
                } catch (Exception e) {
                    Log.e(TAG, "Listener exception (" + w.id + ")", e);
                }
            }
        };

        mHandler.postDelayed(mDebouncedCallback, DEBOUNCE_MS);
    }

    private void cancelPendingCallback() {
        if (mDebouncedCallback != null) {
            mHandler.removeCallbacks(mDebouncedCallback);
            mDebouncedCallback = null;
        }
    }

    private String getTopPackageName() {
        String pkg = getTopPackageNameViaTasks();
        if (pkg != null) {
            return pkg;
        }

        pkg = getTopPackageNameViaUsageStats();
        if (pkg != null) {
            return pkg;
        }

        return null;
    }

    private String getTopPackageNameViaTasks() {
        try {
            List<ActivityManager.RunningTaskInfo> tasks =
                    ActivityTaskManager.getService().getTasks(1, false, false, -1);
            if (tasks != null && !tasks.isEmpty()) {
                ActivityManager.RunningTaskInfo task = tasks.get(0);
                if (task.topActivity != null) {
                    String pkg = task.topActivity.getPackageName();
                    return pkg != null ? pkg.trim() : null;
                }
            }
        } catch (Exception e) {
            if (Constants.DEBUG) Log.i(TAG, "getTasks failed", e);
        }
        return null;
    }

    private String getTopPackageNameViaUsageStats() {
        try {
            UsageStatsManager usm = (UsageStatsManager) mContext.getSystemService(Context.USAGE_STATS_SERVICE);
            if (usm == null) return null;

            long now = System.currentTimeMillis();
            UsageEvents events = usm.queryEvents(now - 10000, now);
            if (events == null) return null;

            String topPackage = null;
            UsageEvents.Event event = new UsageEvents.Event();

            while (events.hasNextEvent()) {
                events.getNextEvent(event);
                if (event.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                    topPackage = event.getPackageName();
                }
            }

            return topPackage != null ? topPackage.trim() : null;
        } catch (Exception e) {
            if (Constants.DEBUG) Log.i(TAG, "UsageStats failed", e);
            return null;
        }
    }

    private void registerTaskStackListenerIfNeeded() {
        if (mTaskStackListener != null) {
            if (Constants.DEBUG) Log.i(TAG, "TaskStackListener already registered");
            return;
        }

        mTaskStackListener = new TaskStackListener() {
            @Override
            public void onTaskStackChanged() {
                if (Constants.DEBUG) Log.i(TAG, "onTaskStackChanged");
                mHandler.post(ForegroundAppDetector.this::handlePossibleTopAppChange);
            }

            @Override
            public void onTaskMovedToFront(ActivityManager.RunningTaskInfo taskInfo) {
                if (Constants.DEBUG) Log.i(TAG, "onTaskMovedToFront: " + taskInfo.topActivity);
                mHandler.post(ForegroundAppDetector.this::handlePossibleTopAppChange);
            }
        };

        try {
            ActivityTaskManager.getService().registerTaskStackListener(mTaskStackListener);
            if (Constants.DEBUG) Log.i(TAG, "TaskStackListener registered successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to register TaskStackListener", e);
            mTaskStackListener = null;
        }
    }

    private void unregisterTaskStackListener() {
        if (mTaskStackListener != null) {
            try {
                ActivityTaskManager.getService().unregisterTaskStackListener(mTaskStackListener);
                if (Constants.DEBUG) Log.i(TAG, "TaskStackListener unregistered");
            } catch (Exception e) {
                Log.e(TAG, "Failed to unregister TaskStackListener", e);
            }
            mTaskStackListener = null;
        }
    }

    private void registerScreenReceiverIfNeeded() {
        if (mScreenReceiver != null) return;

        mScreenReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
                    if (Constants.DEBUG) Log.i(TAG, "Screen on - re-checking app");
                    mHandler.post(ForegroundAppDetector.this::handlePossibleTopAppChange);
                }
            }
        };

        try {
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_SCREEN_ON);
            mContext.registerReceiver(mScreenReceiver, filter);
            if (Constants.DEBUG) Log.i(TAG, "Screen receiver registered");
        } catch (Exception e) {
            Log.e(TAG, "Failed to register screen receiver", e);
            mScreenReceiver = null;
        }
    }

    private void unregisterScreenReceiver() {
        if (mScreenReceiver != null) {
            try {
                mContext.unregisterReceiver(mScreenReceiver);
                if (Constants.DEBUG) Log.i(TAG, "Screen receiver unregistered");
            } catch (Exception e) {
                Log.e(TAG, "Failed to unregister screen receiver", e);
            }
            mScreenReceiver = null;
        }
    }
}
