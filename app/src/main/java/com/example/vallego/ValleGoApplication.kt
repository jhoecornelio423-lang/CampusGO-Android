package com.example.vallego

import android.app.Application
import com.example.vallego.core.di.networkModule
import com.example.vallego.core.di.repositoryModule
import com.example.vallego.core.di.uiModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class ValleGoApplication : Application() {
    override fun attachBaseContext(base: android.content.Context) {
        val configuration = android.content.res.Configuration(base.resources.configuration)
        configuration.uiMode = android.content.res.Configuration.UI_MODE_NIGHT_NO or
                (configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK.inv())
        val context = base.createConfigurationContext(configuration)
        super.attachBaseContext(context)
    }

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@ValleGoApplication)
            modules(networkModule, repositoryModule, uiModule)
        }
        com.example.vallego.core.notification.ValleGoNotificationHelper.createNotificationChannels(this)
        com.example.vallego.data.repository.SellerPaymentMethodsStorage.initialize(this)
    }
}