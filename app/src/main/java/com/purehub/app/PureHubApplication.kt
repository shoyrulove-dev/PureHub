package com.purehub.app

import android.app.Application
import com.purehub.app.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class PureHubApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@PureHubApplication)
            modules(appModule)
        }
    }
}
