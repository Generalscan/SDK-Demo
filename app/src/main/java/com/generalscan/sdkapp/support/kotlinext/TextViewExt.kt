package com.generalscan.sdkapp.support.kotlinext

import android.widget.TextView

/**
 * Created by alexli on 16/6/2017.
 */
var TextView.textTrim: String
    get() = getText().trim().toString()
    set(v) = setText(v.trim())
