package com.neptune.talkplus_calls_android_sample

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.neptune.talkplus_calls_android_sample.databinding.ActivityMainBinding
import com.neptune.talkplus_calls_android_sample.extensions.showToast

class MainActivity : AppCompatActivity() {
    private val binding: ActivityMainBinding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        binding.btnStartCall.setOnClickListener { moveCallActivity() }
    }

    private fun moveCallActivity() = with(binding)  {
        val intent = Intent(this@MainActivity, CallActivity::class.java).apply {
            putExtra(CallActivity.INTENT_EXTRA_CALLER_ID, etCallerId.text.toString())
            putExtra(CallActivity.INTENT_EXTRA_CALLEE_ID, etCalleeId.text.toString())
        }
        val isValid = (etCalleeId.text.toString().trim().isNotEmpty() && etCallerId.text.toString().trim().isNotEmpty())
        if (isValid) startActivity(intent) else showToast("input caller, callee id")
    }

}