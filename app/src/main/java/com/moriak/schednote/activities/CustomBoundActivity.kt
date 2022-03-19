package com.moriak.schednote.activities

import android.os.Bundle
import androidx.annotation.CallSuper
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding

/**
 * Táto aktivita aplikuje viewBinding.
 *
 * @property binding pristupuje k pomenovaným blokom layoutu.
 */
abstract class CustomBoundActivity<T: ViewBinding> : AppCompatActivity() {
    private var _binding: T? = null
    protected val binding: T get() = _binding!!

    /**
     * Vytvori objekt typu ViewBinding
     * @return objekt
     */
    protected abstract fun onCreateBinding(): T

    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = onCreateBinding()
        setContentView(_binding!!.root)
    }

    @CallSuper
    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}