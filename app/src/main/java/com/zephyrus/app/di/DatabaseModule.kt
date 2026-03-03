package com.zephyrus.app.di

import android.content.Context
import androidx.room.Room
import com.zephyrus.app.data.local.SavedLocationDao
import com.zephyrus.app.data.local.ZephyrusDatabase
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
    fun provideDatabase(@ApplicationContext context: Context): ZephyrusDatabase {
        return Room.databaseBuilder(
            context,
            ZephyrusDatabase::class.java,
            "zephyrus.db"
        ).build()
    }

    @Provides
    fun provideSavedLocationDao(database: ZephyrusDatabase): SavedLocationDao {
        return database.savedLocationDao()
    }
}
