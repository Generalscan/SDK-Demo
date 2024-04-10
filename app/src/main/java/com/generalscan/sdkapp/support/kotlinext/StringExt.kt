package com.generalscan.sdkapp.support.kotlinext

/**
 * Created by alexli on 16/6/2017.
 */
fun CharSequence?.ifNullTrim() : String {
    if(this== null)
        return ""
    else
        return this.trim().toString()
}

fun CharSequence?.ifNullOrEmpty(defaultString : CharSequence) : String {
    if(this == null)
        return defaultString.toString()
    else
        return if(this.trim().isEmpty()) defaultString.toString() else this.toString()
}

fun CharSequence?.ifNullOrBlank(defaultString : CharSequence) : String {
    if(this.isNullOrBlank())
        return defaultString.toString()
    else
        return this.toString()
}


fun CharSequence?.isAlpha(): Boolean {
    return this!!.matches("[a-zA-Z]+".toRegex())
}

fun CharSequence?.isAlphaUpper(): Boolean {
    return this!!.matches("[A-Z]+".toRegex())
}


fun CharSequence?.isInteger(): Boolean {
    try {
        Integer.parseInt(this.ifNullTrim())
    } catch (e: NumberFormatException) {
        return false
    } catch (e: NullPointerException) {
        return false
    }
    // only got here if we didn't return false
    return true
}