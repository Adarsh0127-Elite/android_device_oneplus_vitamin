/*
 * Copyright (C) 2025 kenway214
 * Copyright (C) 2026 Evolution X
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.settings.oplusparts.gamebar;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.WindowManager;
import android.window.TaskFpsCallback;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class GameBarFpsMeter {

    private static final String TAG = "GameBarFpsMeter";
    private static final long STALENESS_THRESHOLD_MS = 2000;
    private static final long TASK_CHECK_INTERVAL_MS = 1000;

    private static GameBarFpsMeter sInstance;
    private final WindowManager mWindowManager;
    private float mCurrentFps = 0f;
    private TaskFpsCallback mTaskFpsCallback;
    private boolean mCallbackRegistered = false;
    private int mCurrentTaskId = -1;
    private long mLastFpsUpdateTime = System.currentTimeMillis();
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    public static synchronized GameBarFpsMeter getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new GameBarFpsMeter(context.getApplicationContext());
        }
        return sInstance;
    }

    private GameBarFpsMeter(Context context) {
        mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            mTaskFpsCallback = new TaskFpsCallback() {
                @Override
                public void onFpsReported(float fps) {
                    if (fps > 0) {
                        mCurrentFps = fps;
                        mLastFpsUpdateTime = System.currentTimeMillis();
                    }
                }
            };
        }
    }

    public void start() {
        stop();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && mWindowManager != null && mTaskFpsCallback != null) {
            int taskId = getFocusedTaskId();
            if (taskId <= 0) {
                mHandler.postDelayed(mTaskCheckRunnable, TASK_CHECK_INTERVAL_MS);
                return;
            }
            mCurrentTaskId = taskId;
            try {
                mWindowManager.registerTaskFpsCallback(mCurrentTaskId, Runnable::run, mTaskFpsCallback);
                mCallbackRegistered = true;
            } catch (Exception e) {
                Log.w(TAG, "Failed to register TaskFpsCallback for task " + mCurrentTaskId, e);
            }
            mLastFpsUpdateTime = System.currentTimeMillis();
            mHandler.postDelayed(mTaskCheckRunnable, TASK_CHECK_INTERVAL_MS);
        }
    }

    public void stop() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && mWindowManager != null && mTaskFpsCallback != null) {
            if (mCallbackRegistered) {
                try {
                    mWindowManager.unregisterTaskFpsCallback(mTaskFpsCallback);
                } catch (Exception ignored) {
                }
                mCallbackRegistered = false;
            }
            mHandler.removeCallbacks(mTaskCheckRunnable);
        }
    }

    public float getFps() {
        return mCurrentFps;
    }

    private final Runnable mTaskCheckRunnable = new Runnable() {
        @Override
        public void run() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                int newTaskId = getFocusedTaskId();
                if (newTaskId > 0 && newTaskId != mCurrentTaskId) {
                    reinitCallback();
                } else {
                    long now = System.currentTimeMillis();
                    if (now - mLastFpsUpdateTime > STALENESS_THRESHOLD_MS) {
                        reinitCallback();
                    }
                }
                mHandler.postDelayed(this, TASK_CHECK_INTERVAL_MS);
            }
        }
    };

    private int getFocusedTaskId() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return -1;
        }
        try {
            Class<?> atmClass = Class.forName("android.app.ActivityTaskManager");
            Method getServiceMethod = atmClass.getDeclaredMethod("getService");
            Object atmService = getServiceMethod.invoke(null);
            Method getFocusedRootTaskInfoMethod = atmService.getClass().getMethod("getFocusedRootTaskInfo");
            Object taskInfo = getFocusedRootTaskInfoMethod.invoke(atmService);
            if (taskInfo != null) {
                try {
                    Field taskIdField = taskInfo.getClass().getField("taskId");
                    return taskIdField.getInt(taskInfo);
                } catch (NoSuchFieldException nsfe) {
                    try {
                        Field taskIdField = taskInfo.getClass().getField("mTaskId");
                        return taskIdField.getInt(taskInfo);
                    } catch (NoSuchFieldException ignored) {
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return -1;
    }

    private void reinitCallback() {
        stop();
        mHandler.postDelayed(this::start, 500);
    }
}
