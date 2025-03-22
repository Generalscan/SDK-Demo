package com.generalscan.sdkapp.ui.activity.base

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import com.generalscan.sdkapp.support.inject.InjectUtility
import java.util.HashMap

/**
 * Created by wangdan on 15-1-16.
 */
open class BaseActivity : AppCompatActivity() {

    var isDestory: Boolean = false

    var rootView: View? = null
        private set

    var toolbar: Toolbar? = null
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onBackPressedDispatcher.addCallback(this /* lifecycle owner */, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Back is pressed... Finishing the activity
                handleBackPressed()
            }
        })
    }


    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

    }

    override fun onStart() {
        super.onStart()

    }

    override fun onRestart() {
        super.onRestart()

    }

    override fun setContentView(layoutResID: Int) {
        setContentView(View.inflate(this, layoutResID, null))
    }

    override fun setContentView(view: View?, params: ViewGroup.LayoutParams?) {
        super.setContentView(view, params)
        supportActionBar?.elevation = 0f
        rootView = view
        InjectUtility.initInjectedView(this)
    }

    override fun setContentView(view: View?) {
        super.setContentView(view, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        supportActionBar?.elevation = 0f
        rootView = view
        InjectUtility.initInjectedView(this)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                this.finish()
                true
            }
            else -> true
        }
    }
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

    }

    public override fun onDestroy() {

        isDestory = true

        super.onDestroy()

    }

    protected open fun shouldFireBackPress():Boolean {
        return true
    }
    protected open fun handleBackPressed()
    {
        if(shouldFireBackPress())
            finish()
    }

}
