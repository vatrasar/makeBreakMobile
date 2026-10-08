package com.example.makebreak.core.di

import android.content.Context
import androidx.room.Room
import com.example.makebreak.core.data.local.MakeBreakDb
import com.example.makebreak.core.data.local.dao.UserSettingsDao
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
    fun provideMakeBreakDb(
        @ApplicationContext context: Context
    ): MakeBreakDb {
        return Room.databaseBuilder(
            context,
            MakeBreakDb::class.java,
            MakeBreakDb.DATABASE_NAME
        ).build()
    }

    @Provides
    @Singleton
    fun provideUserSettingsDao(
        database: MakeBreakDb
    ): UserSettingsDao {
        return database.userSettingsDao()
    }
}
