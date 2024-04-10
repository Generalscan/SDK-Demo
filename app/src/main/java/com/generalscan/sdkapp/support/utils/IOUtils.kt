package com.generalscan.sdkapp.support.utils

import java.util.Date

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.text.format.DateUtils
import android.webkit.MimeTypeMap
import com.generalscan.sdkapp.support.kotlinext.readByteArrayAndClose
import com.generalscan.sdkapp.support.kotlinext.readTextAndClose
import com.generalscan.sdkapp.support.kotlinext.toFile
import java.io.*

object IOUtils {

    private val TAG = IOUtils::class.java.simpleName


    //region SD Card functions
    fun hasSDCard(): Boolean {
        var mHasSDcard = false
        if (Environment.MEDIA_MOUNTED.endsWith(Environment.getExternalStorageState())) {
            mHasSDcard = true
        } else {
            mHasSDcard = false
        }

        return mHasSDcard
    }

    fun geSsdcardPath(): String {
       return if (hasSDCard()) Environment.getExternalStorageDirectory().absolutePath else "/sdcard/"
    }

    fun sdcardCanWrite(): Boolean {
        return Environment.getExternalStorageDirectory().canWrite()
    }

    fun hasSdcardAndCanWrite(): Boolean {
        return hasSDCard() && sdcardCanWrite()
    }
    //endregion
    fun createFolder(folder: String) {
        val tempDir = File(folder)
        if (!tempDir.exists())
            tempDir.mkdirs()
    }


    fun copyFile(inputStream: InputStream, outputStream: OutputStream) {
        inputStream.use { input ->
            outputStream.use { fileOut ->
                input.copyTo(fileOut)
            }
        }
    }

    fun combinePath(vararg args: String): String {
        var path = ""
        args.forEach {
            var arg = it
            if (path.isEmpty())
                path += arg
            else {
                if (!path.endsWith("/"))
                    path += "/"
                if (arg.startsWith("/"))
                    arg = arg.substring(1)
                path += arg
            }
        }
        return path
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


    fun readAssetsFile(file: String, context: Context): String {
        try {
            val ins = context.resources.assets.open(file)
            return ins.readTextAndClose()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return ""
    }

    fun readFileToString(file: File): String? {
        val sb = StringBuffer()
        try {
            return FileInputStream(file).readTextAndClose()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun readFileToBytes(file: File): ByteArray? {
        try {
            return FileInputStream(file).readByteArrayAndClose()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return null
    }

    fun writeFile(ins: InputStream, file: File) {
        if (!file.parentFile.exists())
            file.parentFile.mkdirs()

        try {
            ins.use {
                it.toFile(file)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    fun writeFile(file: File, content: String): Boolean {
        if (!file.parentFile.exists())
            file.parentFile.mkdirs()

        var out: FileOutputStream? = null
        try {
            out = FileOutputStream(file)
            out.use {
                it.write(content.toByteArray())
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
        return true
    }

    fun writeFile(file: File, bytes: ByteArray): Boolean {
        if (!file.parentFile.exists())
            file.parentFile.mkdirs()
        try {
            ByteArrayInputStream(bytes).use {
                it.toFile(file)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }

        return true
    }

    @Throws(Exception::class)
    fun copyFile(sourceFile: File, targetFile: File) {
        FileInputStream(sourceFile).use {
            it.toFile(targetFile)
        }
    }


    fun <T> readObject(file: File, clazz: Class<T>): T? {
        try {
            ObjectInputStream(FileInputStream(file)).use {
                return it.readObject() as T
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun writeObject(file: File, o: Serializable) {
        if (!file.parentFile.exists())
            file.parentFile.mkdirs()
        try {
            FileOutputStream(file).use {
                ObjectOutputStream(it).use { out ->
                    out.writeObject(o)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    fun getDataColumn(context: Context, uri: Uri, selection: String, selectionArgs: Array<String>): String? {

        var cursor: Cursor? = null
        val column = "_data"
        val projection = arrayOf(column)

        try {
            cursor = context.contentResolver.query(uri, projection, selection, selectionArgs, null)
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndexOrThrow(column)
                return cursor.getString(index)
            }
        } finally {
            if (cursor != null)
                cursor.close()
        }
        return null
    }

    /**
     * @param uri
     * The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    fun isExternalStorageDocument(uri: Uri): Boolean {
        return "com.android.externalstorage.documents" == uri.authority
    }

    fun getMimeType(url: String): String? {
        var type: String? = null
        val extension = MimeTypeMap.getFileExtensionFromUrl(url)
        if (extension != null) {
            val mime = MimeTypeMap.getSingleton()
            type = mime.getMimeTypeFromExtension(extension)
        }
        return type
    }


    /*
     * gets the last path component
     *
     * Example: getLastPathComponent("downloads/example/file");
     * Result: "file"
     */
    fun getLastPathComponent(filePath: String): String {
        val segments = filePath.split("/")
        return if (segments.isEmpty()) "" else segments[segments.size - 1]
    }
}
