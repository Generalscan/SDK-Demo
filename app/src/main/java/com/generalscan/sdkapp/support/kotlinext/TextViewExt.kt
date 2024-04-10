package com.generalscan.sdkapp.support.kotlinext

import android.widget.TextView

/**
 * Created by alexli on 16/6/2017.
 */
var TextView.textTrim: String
    get() = getText().trim().toString()
    set(v) = setText(v.trim())

var TextView.textTrimUpper: String
    get() = getText().trim().toString().toUpperCase()
    set(v) = setText(v.trim().toUpperCase())


fun TextView.getTextTrim(uppcase: Boolean) : String {
    if(uppcase)
        return this.textTrimUpper
    else
        return this.textTrim
}

fun TextView.isNullOrEmpty() : Boolean {
    return this.text.isNullOrEmpty()
}

fun TextView.isNullOrBlank() : Boolean {
    return this.text.isNullOrBlank()
}