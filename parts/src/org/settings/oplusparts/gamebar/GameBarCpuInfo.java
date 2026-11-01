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

import android.util.Log;

import org.settings.oplusparts.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class GameBarCpuInfo {

    private static final String TAG = "GameBarCpuInfo";

    private static long sPrevIdle = -1;
    private static long sPrevTotal = -1;

    private static final String CPU_SYSFS = "/sys/devices/system/cpu/";
    private static String sCachedCpuTempZone = null;

    public static String getCpuUsage() {
        String line = readLine("/proc/stat");
        if (line == null || !line.startsWith("cpu ")) return "N/A";
        String[] parts = line.split("\\s+");
        if (parts.length < 8) return "N/A";

        try {
            long user    = Long.parseLong(parts[1]);
            long nice    = Long.parseLong(parts[2]);
            long system  = Long.parseLong(parts[3]);
            long idle    = Long.parseLong(parts[4]);
            long iowait  = Long.parseLong(parts[5]);
            long irq     = Long.parseLong(parts[6]);
            long softirq = Long.parseLong(parts[7]);
            long steal   = parts.length > 8 ? Long.parseLong(parts[8]) : 0;

            long total = user + nice + system + idle + iowait + irq + softirq + steal;

            if (sPrevTotal != -1 && total != sPrevTotal) {
                long diffTotal = total - sPrevTotal;
                long diffIdle  = idle - sPrevIdle;
                int usage = (int) (100 * (diffTotal - diffIdle) / (double) diffTotal);
                sPrevTotal = total;
                sPrevIdle  = idle;
                return String.valueOf(Math.max(0, Math.min(100, usage)));
            } else {
                sPrevTotal = total;
                sPrevIdle  = idle;
                return "N/A";
            }
        } catch (NumberFormatException e) {
            Log.w(TAG, "Failed parsing /proc/stat", e);
            return "N/A";
        }
    }

    public static List<String> getCpuFrequencies() {
        List<String> result = new ArrayList<>();
        File cpuDir = new File(CPU_SYSFS);
        File[] files = cpuDir.listFiles((dir, name) -> name.matches("cpu\\d+"));
        if (files == null || files.length == 0) {
            return result;
        }

        List<File> cpuFolders = new ArrayList<>();
        Collections.addAll(cpuFolders, files);
        cpuFolders.sort(Comparator.comparingInt(GameBarCpuInfo::extractCpuNumber));

        for (File cpu : cpuFolders) {
            String cpuName = cpu.getName();
            String freq = readCpuFreqForCpu(cpu);
            if (freq == null) {
                result.add(cpuName + ": offline");
            } else {
                result.add(cpuName + ": " + freq + " MHz");
            }
        }

        return result;
    }

    public static int getMaxCpuMhz() {
        List<String> freqs = getCpuFrequencies();
        int best = -1;
        for (String s : freqs) {
            int colon = s.indexOf(':');
            String tail = colon >= 0 ? s.substring(colon + 1).trim() : s.trim();
            StringBuilder digits = new StringBuilder();
            for (int i = 0; i < tail.length(); i++) {
                char c = tail.charAt(i);
                if (Character.isDigit(c)) digits.append(c);
                else if (digits.length() > 0) break;
            }
            if (digits.length() > 0) {
                try {
                    int mhz = Integer.parseInt(digits.toString());
                    if (mhz > best) best = mhz;
                } catch (NumberFormatException ignored) { }
            }
        }
        return best;
    }

    public static String getCpuTemp() {
        String tempPath = getCpuTempPath();
        if (tempPath == null) return "N/A";

        String line = readLine(tempPath);
        if (line == null) return "N/A";
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

    private static String getCpuTempPath() {
        if (sCachedCpuTempZone != null && FileUtils.fileExists(sCachedCpuTempZone)) {
            return sCachedCpuTempZone;
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
                        if (type.equals("soc_max") || type.contains("cpu") || type.contains("mtktscpu")) {
                            File tempFile = new File(zone, "temp");
                            if (tempFile.exists()) {
                                sCachedCpuTempZone = tempFile.getAbsolutePath();
                                return sCachedCpuTempZone;
                            }
                        }
                    }
                }
            }
        }

        if (FileUtils.fileExists("/sys/class/thermal/thermal_zone0/temp")) {
            sCachedCpuTempZone = "/sys/class/thermal/thermal_zone0/temp";
            return sCachedCpuTempZone;
        }

        return null;
    }

    private static String readCpuFreqForCpu(File cpuFolder) {
        String[] candidates = new String[] {
            cpuFolder.getAbsolutePath() + "/cpufreq/scaling_cur_freq",
            cpuFolder.getAbsolutePath() + "/cpufreq/cpuinfo_cur_freq",
            cpuFolder.getAbsolutePath() + "/cpufreq/scaling_max_freq"
        };

        for (String p : candidates) {
            String v = readLine(p);
            if (v == null) continue;
            v = v.trim();
            if (v.isEmpty()) continue;

            StringBuilder digits = new StringBuilder();
            for (int i = 0; i < v.length(); i++) {
                char c = v.charAt(i);
                if (Character.isDigit(c)) digits.append(c);
                else if (digits.length() > 0) break;
            }
            if (digits.length() == 0) continue;
            try {
                int khz = Integer.parseInt(digits.toString());
                int mhz = khz >= 1000 ? khz / 1000 : khz;
                return String.valueOf(mhz);
            } catch (NumberFormatException e) {
                return v;
            }
        }
        return null;
    }

    private static int extractCpuNumber(File cpuFolder) {
        String name = cpuFolder.getName().replace("cpu", "");
        try { return Integer.parseInt(name); }
        catch (NumberFormatException e) { return -1; }
    }

    private static String readLine(String path) {
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            return br.readLine();
        } catch (IOException e) {
            return null;
        }
    }
}
