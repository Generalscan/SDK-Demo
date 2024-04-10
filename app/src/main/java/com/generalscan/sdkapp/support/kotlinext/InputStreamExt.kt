package com.generalscan.sdkapp.support.kotlinext

import java.io.File
import java.io.InputStream
import java.nio.charset.Charset

/**
 * Created by alexli on 12/12/2017.
 */
fun InputStream.readTextAndClose(charset: Charset = Charsets.UTF_8): String {
    return this.bufferedReader(charset).use { it.readText() }
}

fun InputStream.readByteArrayAndClose():ByteArray {
    return this.use { it.readBytes() }
}

fun InputStream.toFile(path: String) {
    File(path).outputStream().use { this.copyTo(it) }
}

fun InputStream.toFile(file: File) {
    file.outputStream().use { this.copyTo(it) }
}

