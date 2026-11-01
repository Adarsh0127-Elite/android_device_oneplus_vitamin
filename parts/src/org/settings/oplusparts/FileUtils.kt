/*
 * Copyright (C) 2018-2026 The LineageOS Project
 * Copyright (C) 2026 Evolution X
 * SPDX-License-Identifier: Apache-2.0
 */

package org.settings.oplusparts

import android.util.Log
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException

object FileUtils {
    private const val TAG = "FileUtils"

    @JvmStatic
    fun readLine(fileName: String?): String? = readLine(fileName, false)

    @JvmStatic
    fun readLine(fileName: String?, trim: Boolean): String? {
        if (fileName == null) return null
        return try {
            BufferedReader(FileReader(fileName), 512).use { reader ->
                val line = reader.readLine()
                if (trim && line != null) line.trim() else line
            }
        } catch (e: Exception) {
            if (Constants.DEBUG) {
                Log.w(TAG, "Could not read from file $fileName: ${e.message}")
            }
            null
        }
    }

    @JvmStatic
    fun readOneLine(path: String?): String? = readLine(path, true)

    @JvmStatic
    fun readLineTrimmed(path: String?): String? = readLine(path, true)

    @JvmStatic
    fun writeLine(fileName: String?, value: String?): Boolean {
        if (fileName == null || value == null) return false
        return try {
            BufferedWriter(FileWriter(fileName)).use { writer ->
                writer.write(value)
                writer.flush()
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Could not write to file $fileName", e)
            false
        }
    }

    @JvmStatic
    fun fileExists(fileName: String?): Boolean {
        if (fileName == null) return false
        return File(fileName).exists()
    }

    @JvmStatic
    fun isFileReadable(fileName: String?): Boolean {
        if (fileName == null) return false
        val file = File(fileName)
        return file.exists() && file.canRead()
    }

    @JvmStatic
    fun isFileWritable(fileName: String?): Boolean {
        if (fileName == null) return false
        val file = File(fileName)
        return file.exists() && file.canWrite()
    }

    @JvmStatic
    fun getFileValue(filename: String?, defValue: String): String {
        return readLine(filename) ?: defValue
    }

    @JvmStatic
    fun getFileValueAsBoolean(filename: String?, defValue: Boolean): Boolean {
        val v = readLine(filename) ?: return defValue
        return v != "0"
    }

    @JvmStatic
    fun getFileValueAsInt(filename: String?, defValue: Int): Int {
        val v = readLineTrimmed(filename) ?: return defValue
        return try {
            v.toInt()
        } catch (e: NumberFormatException) {
            Log.w(TAG, "Unable to parse int from $filename: $v")
            defValue
        }
    }
}
