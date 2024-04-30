package com.neptune.talkplus_calls_android_sample.app

import android.app.Application
import com.neptune.talkplus_calls_android_sample.Constant.TEST_APP_ID
import io.talkplus.TalkPlus

class TPApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        TalkPlus.init(applicationContext, TEST_APP_ID)
    }
}