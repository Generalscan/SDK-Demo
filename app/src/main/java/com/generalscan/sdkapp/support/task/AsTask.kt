package com.generalscan.sdkapp.support.task

import android.app.ProgressDialog
import android.content.Context
import android.os.AsyncTask
import android.util.Log
import com.generalscan.sdkapp.support.utils.Utils

class AsTask(val context: Context, val task: (Context) -> CallResult, val onFinish: (Context, CallResult) -> Unit, val loadMessage: String?) : AsyncTask<Void, Void, CallResult>() {
    private var progressDialog: ProgressDialog? = null

    constructor(context: Context, task: (Context) -> CallResult, onFinish: (Context, CallResult) -> Unit, loadMessageResId: Int)
            : this(context, task, onFinish, context.getString(loadMessageResId))

    init {
        if (!this.loadMessage.isNullOrBlank()) {
            progressDialog = ProgressDialog(context)
            progressDialog!!.setMessage(this.loadMessage)
            progressDialog!!.isIndeterminate = true
            progressDialog!!.setCancelable(false)
        }
    }
    companion object {
        private val TAG = "KillerTask"
    }

    /**
     * Override AsyncTask's function doInBackground
     */
    override fun doInBackground(vararg params: Void): CallResult {
        var callResult = CallResult()
        try {
            Log.wtf(TAG, "Enter to doInBackground")
            callResult = run { task(context) }
            //return run { task(context) }
        } catch (e: Exception) {
            Log.wtf(TAG, "Error in background task")
            callResult.isSuccess = false;
            callResult.errorMessage = Utils.getErrorMessage(e)
            callResult.exception = e;
        }
        return callResult;
    }

    override fun onPreExecute() {
        if (progressDialog != null) {
            progressDialog!!.show()
        }
    }
    /**
     * Override AsyncTask's function onPostExecute
     */
    override fun onPostExecute(result: CallResult) {
        Log.wtf(TAG, "Enter to onPostExecute")

        try {
            run {onFinish(context, result)}
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }

        if (progressDialog != null && progressDialog!!.isShowing()) {
            progressDialog!!.dismiss()
            progressDialog = null
        }
    }

    /**
     * Execute AsyncTask
     */
    fun go() {
        execute()
    }

    /**
     * Cancel AsyncTask
     */
    fun cancel() {
        cancel(true)
    }

}