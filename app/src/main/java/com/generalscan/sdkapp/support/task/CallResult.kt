package com.generalscan.sdkapp.support.task

/**
 * Created by alexli on 11/26/13.
 */
open class CallResult {
    var errorMessage: String = ""
        set(errorMessage) {
            this.isSuccess = false
            field = errorMessage
        }
    var errorCode: String? = ""
    var result: Any? = null
    var isSuccess = true
    var exception: Throwable? = null
}
