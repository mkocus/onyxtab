package com.n7mobile.onyxtab

import android.app.Application
import com.onyx.android.sdk.rx.RxBaseAction
import com.onyx.android.sdk.rx.RxManager
import com.onyx.android.sdk.utils.ResManager
import org.lsposed.hiddenapibypass.HiddenApiBypass

class MyApp : Application() {

    override fun onCreate() {
        super.onCreate()
        RxManager.Builder.initAppContext(this)
        ResManager.init(this)
        RxBaseAction.init(this)
        HiddenApiBypass.addHiddenApiExemptions("")
    }
}