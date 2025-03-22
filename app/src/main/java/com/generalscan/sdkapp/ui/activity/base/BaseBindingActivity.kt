package com.generalscan.quickpair.ui.activity.base

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding
import com.generalscan.sdkapp.support.utils.Utils


/**
 * Created by wangdan on 15-1-16.
 */
@SuppressLint("Registered")
abstract class BaseBindingActivity<ViewBindingType : ViewBinding>: AppCompatActivity(){

    companion object {
        private const val TAG = "BindingActivity"
    }

    // Variables
    private var _binding: ViewBindingType? = null

    // Binding variable to be used for accessing views.
    protected val binding
        get() = requireNotNull(_binding)

    /*
     * Calls the abstract function to return the ViewBinding and set up LifeCycle Observer to get
     * rid of binding once done.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            super.onCreate(savedInstanceState)
            _binding = setupViewBinding(layoutInflater)
            beforeCreate()
            setContentView(requireNotNull(_binding).root)
            /*
            toolbar = findViewById(R.id.toolbar)
            if (toolbar != null) {
                setSupportActionBar(toolbar)
            }
             */
            afterCreate(savedInstanceState)
        } catch (e: Throwable) {
            e.printStackTrace()
            showWarningMessage(Utils.getErrorMessage(e))
        }

    }

    abstract fun beforeCreate()

    abstract fun afterCreate(savedInstanceState: Bundle?)

    abstract fun setupViewBinding(inflater: LayoutInflater): ViewBindingType

    /*
     * Safe call method, just in case, if anything is messed up and lifecycle Event does not gets
     * called.
     */
    override fun onDestroy() {
        _binding = null
        super.onDestroy()
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
    protected fun showMessage(message: CharSequence) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    protected  fun showWarningMessage(msg: String?) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }

}
