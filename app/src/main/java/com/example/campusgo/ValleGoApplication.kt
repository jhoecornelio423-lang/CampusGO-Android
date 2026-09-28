package com.example.campusgo

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import com.example.campusgo.core.di.networkModule
import com.example.campusgo.core.di.repositoryModule
import com.example.campusgo.core.di.uiModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class ValleGoApplication : Application(), ImageLoaderFactory {
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
        com.example.campusgo.core.notification.ValleGoNotificationHelper.createNotificationChannels(this)
        com.example.campusgo.data.repository.SellerPaymentMethodsStorage.initialize(this)
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("vallego_image_cache"))
                    .maxSizeBytes(250L * 1024 * 1024) // Límite máximo de seguridad: 250 MB
                    .build()
            }
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            .respectCacheHeaders(false) // Servir siempre desde el almacenamiento local si ya fue descargada
            .crossfade(true)
            .build()
    }
}