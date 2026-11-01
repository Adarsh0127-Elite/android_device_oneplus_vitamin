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

import org.settings.oplusparts.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Locale;

public class GameBarGpuInfo {

    private static final String[] GPU_USAGE_PATHS = {
            "/sys/kernel/ged/hal/gpu_utilization",
            "/sys/module/ged/parameters/gpu_loading",
            "/proc/gpufreqv2/gpu_loading",
            "/sys/class/misc/mali0/device/utilization",
            "/sys/class/devfreq/13000000.mali/load"
    };

    private static final String[] GPU_CLOCK_PATHS = {
            "/proc/gpufreqv2/cur_freq",
            "/sys/class/devfreq/13000000.mali/cur_freq",
            "/sys/kernel/ged/hal/current_freq"
    };

    private static String sCachedGpuTempZone = null;

    public static String getGpuUsage() {
        for (String path : GPU_USAGE_PATHS) {
            String line = readLine(path);
            if (line != null) {
                line = line.replace("%", "").trim();
                try {
                    int val = Integer.parseInt(line);
                    return String.valueOf(Math.max(0, Math.min(100, val)));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return "N/A";
    }

    public static String getGpuClock() {
        for (String path : GPU_CLOCK_PATHS) {
            String line = readLine(path);
            if (line != null) {
                line = line.trim();
                try {
                    long hz = Long.parseLong(line);
                    long mhz = hz >= 1_000_000 ? hz / 1_000_000 : (hz >= 1000 ? hz / 1000 : hz);
                    return String.valueOf(mhz);
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return "N/A";
    }

    public static String getGpuTemp() {
        String path = getGpuTempPath();
        if (path == null) {
            // Fallback to CPU/SoC max temp on unified thermal designs
            return GameBarCpuInfo.getCpuTemp();
        }

        String line = readLine(path);
        if (line == null) {
            return GameBarCpuInfo.getCpuTemp();
        }

        line = line.trim();
        try {
            float raw = Float.parseFloat(line);
            float c;
            if (raw > 1000) {
                c = raw / 1000f;
            } else if (raw > 100) {
                c = raw / 10f;
            } else {
                c = raw;
            }
            return String.format(Locale.getDefault(), "%.1f", c);
        } catch (NumberFormatException e) {
            return "N/A";
        }
    }

    private static String getGpuTempPath() {
        if (sCachedGpuTempZone != null && FileUtils.fileExists(sCachedGpuTempZone)) {
            return sCachedGpuTempZone;
        }

        File thermalDir = new File("/sys/class/thermal");
        File[] zones = thermalDir.listFiles((dir, name) -> name.startsWith("thermal_zone"));
        if (zones != null) {
            for (File zone : zones) {
                File typeFile = new File(zone, "type");
                if (typeFile.exists()) {
                    String type = readLine(typeFile.getAbsolutePath());
                    if (type != null) {
                        type = type.trim().toLowerCase(Locale.ROOT);
                        if (type.contains("gpu") || type.contains("mali")) {
                            File tempFile = new File(zone, "temp");
                            if (tempFile.exists()) {
                                sCachedGpuTempZone = tempFile.getAbsolutePath();
                                return sCachedGpuTempZone;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private static String readLine(String path) {
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            return br.readLine();
        } catch (IOException e) {
            return null;
        }
    }
}
