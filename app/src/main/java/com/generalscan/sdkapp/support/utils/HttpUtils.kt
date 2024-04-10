package com.generalscan.sdkapp.support.utils

import com.generalscan.sdkapp.support.kotlinext.readTextAndClose
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL


/**
 * Created by alexli on 22/2/2018.
 */
object HttpUtils
{

    @Throws(Exception::class)
    fun getOnlineFileContent(url: String): String {
        var DataInputStream: InputStream? = null
        try {

            val url = URL(url)
            val cc = url.openConnection() as HttpURLConnection
            //set timeout for reading InputStream
            cc.setReadTimeout(5000)
            // set timeout for connection
            cc.setConnectTimeout(5000)
            //set HTTP method to GET
            cc.setRequestMethod("GET")
            //set it to true as we are connecting for input
            cc.setDoInput(true)

            //reading HTTP response code
            val response = cc.getResponseCode()

            //if response code is 200 / OK then read Inputstream
            if (response == HttpURLConnection.HTTP_OK) {
                DataInputStream = cc.getInputStream()
            }

        } catch (e: Exception) {
            e.printStackTrace();
        }
        if(DataInputStream== null)
            return "";
        return DataInputStream!!.readTextAndClose()
    }
}