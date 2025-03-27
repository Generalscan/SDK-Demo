package com.generalscan.sdkapp.support.utils

import java.util.Date

import android.text.format.DateUtils
import java.io.*

object IOUtils {

    private val TAG = IOUtils::class.java.simpleName


    fun createFolder(folder: String) {
        val tempDir = File(folder)
        if (!tempDir.exists())
            tempDir.mkdirs()
    }

    fun clearFolder(folder: File?, numDays: Int): Int {
        var deletedFiles = 0
        if (folder != null && folder.isDirectory) {
            try {
                for (child in folder.listFiles()) {
                    if (child.isDirectory) {
                        deletedFiles += clearFolder(child, numDays)
                    }
                    if (child.lastModified() < Date().time - numDays * DateUtils.DAY_IN_MILLIS) {
                        if (child.delete()) {
                            deletedFiles++
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }
        return deletedFiles
    }
}
