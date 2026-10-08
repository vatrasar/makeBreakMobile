package com.example.makebreak.core.di

import com.example.makebreak.core.data.repository.UserSettingsRepositoryImpl
import com.example.makebreak.core.domain.repository.UserSettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindUserSettingsRepository(
        userSettingsRepositoryImpl: UserSettingsRepositoryImpl
    ): UserSettingsRepository
}
