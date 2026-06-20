package com.movie.app.best.di

import android.content.Context
import com.movie.app.best.MovieApplication
import com.ketch.Ketch
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object KetchModule {

    @Provides
    @Singleton
    fun provideKetch(@ApplicationContext context: Context): Ketch {
        return (context as MovieApplication).ketch
    }
}
