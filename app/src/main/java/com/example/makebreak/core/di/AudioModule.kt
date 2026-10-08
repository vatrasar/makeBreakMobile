package com.example.makebreak.core.di

import com.example.makebreak.core.data.audio.AudioAnnouncementPlayerImpl
import com.example.makebreak.core.domain.service.AudioAnnouncementPlayer
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AudioModule {

    @Binds
    @Singleton
    abstract fun bindAudioAnnouncementPlayer(
        impl: AudioAnnouncementPlayerImpl
    ): AudioAnnouncementPlayer
}
