package com.example.campusgo.core.di

import androidx.room.Room
import com.example.campusgo.data.local.database.CampusGoDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val databaseModule = module {
    single<CampusGoDatabase> {
        Room.databaseBuilder(
            androidContext(),
            CampusGoDatabase::class.java,
            "campusgo_offline.db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    single { get<CampusGoDatabase>().productDao() }
    single { get<CampusGoDatabase>().orderDao() }
}
