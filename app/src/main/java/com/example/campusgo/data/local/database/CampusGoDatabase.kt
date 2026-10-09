package com.example.campusgo.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.campusgo.data.local.dao.OrderDao
import com.example.campusgo.data.local.dao.ProductDao
import com.example.campusgo.data.local.entity.OrderEntity
import com.example.campusgo.data.local.entity.ProductEntity

@Database(
    entities = [ProductEntity::class, OrderEntity::class],
    version = 1,
    exportSchema = false
)
abstract class CampusGoDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun orderDao(): OrderDao
}
