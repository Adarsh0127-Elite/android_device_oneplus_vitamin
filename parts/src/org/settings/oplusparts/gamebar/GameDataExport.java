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

import android.os.Environment;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class GameDataExport {

    private static final String TAG = "GameDataExport";

    private static GameDataExport sInstance;
    public static synchronized GameDataExport getInstance() {
        if (sInstance == null) {
            sInstance = new GameDataExport();
        }
        return sInstance;
    }

    private boolean mCapturing = false;

    private final List<String[]> mStatsRows = new ArrayList<>();

    private static final String[] CSV_HEADER = {
            "DateTime",
            "PackageName",
            "FPS",
            "Battery_Temp",
            "CPU_Usage",
            "CPU_Temp",
            "GPU_Usage",
            "GPU_Clock",
            "GPU_Temp"
    };

    private GameDataExport() {
    }

    public void startCapture() {
        mCapturing = true;
        mStatsRows.clear();
        mStatsRows.add(CSV_HEADER);
    }

    public void stopCapture() {
        mCapturing = false;
    }

    public boolean isCapturing() {
        return mCapturing;
    }

    public void addOverlayData(String dateTime,
                               String packageName,
                               String fps,
                               String batteryTemp,
                               String cpuUsage,
                               String cpuTemp,
                               String gpuUsage,
                               String gpuClock,
                               String gpuTemp) {
        if (!mCapturing) return;

        String[] row = {
                dateTime,
                packageName,
                fps,
                batteryTemp,
                cpuUsage,
                cpuTemp,
                gpuUsage,
                gpuClock,
                gpuTemp
        };
        mStatsRows.add(row);
    }

    public String exportDataToCsv() {
        if (mStatsRows.size() <= 1) {
            return null;
        }

        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        if (!downloadsDir.exists()) {
            downloadsDir.mkdirs();
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File csvFile = new File(downloadsDir, "GameBar_Data_" + timeStamp + ".csv");

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(csvFile))) {
            for (String[] row : mStatsRows) {
                StringBuilder line = new StringBuilder();
                for (int i = 0; i < row.length; i++) {
                    line.append(escapeSpecialCharacters(row[i]));
                    if (i < row.length - 1) {
                        line.append(",");
                    }
                }
                bw.write(line.toString());
                bw.newLine();
            }
            bw.flush();
            return csvFile.getAbsolutePath();
        } catch (IOException e) {
            Log.e(TAG, "Error exporting CSV", e);
            return null;
        }
    }

    private String escapeSpecialCharacters(String data) {
        if (data == null) {
            return "";
        }
        String escapedData = data.replaceAll("\\R", " ");
        if (data.contains(",") || data.contains("\"") || data.contains("'")) {
            data = data.replace("\"", "\"\"");
            escapedData = "\"" + data + "\"";
        }
        return escapedData;
    }
}
