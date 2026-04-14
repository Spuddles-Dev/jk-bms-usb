package com.horse.jk_bms.di

import android.content.Context
import androidx.room.Room
import com.horse.jk_bms.data.local.JkBmsDatabase
import com.horse.jk_bms.data.local.dao.ConfigDao
import com.horse.jk_bms.data.local.dao.DeviceInfoDao
import com.horse.jk_bms.data.local.dao.FaultDao
import com.horse.jk_bms.data.local.dao.RuntimeDataDao
import com.horse.jk_bms.data.local.dao.SystemLogDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): JkBmsDatabase {
        return Room.databaseBuilder(
            context,
            JkBmsDatabase::class.java,
            "jk-bms-db",
        ).build()
    }

    @Provides
    fun provideRuntimeDataDao(db: JkBmsDatabase): RuntimeDataDao = db.runtimeDataDao()

    @Provides
    fun provideConfigDao(db: JkBmsDatabase): ConfigDao = db.configDao()

    @Provides
    fun provideDeviceInfoDao(db: JkBmsDatabase): DeviceInfoDao = db.deviceInfoDao()

    @Provides
    fun provideFaultDao(db: JkBmsDatabase): FaultDao = db.faultDao()

    @Provides
    fun provideSystemLogDao(db: JkBmsDatabase): SystemLogDao = db.systemLogDao()
}
