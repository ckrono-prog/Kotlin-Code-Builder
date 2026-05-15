package com.vibehub.di

import android.content.Context
import androidx.room.Room
import com.vibehub.data.local.dao.*
import com.vibehub.data.local.database.VibeHubDatabase
import com.vibehub.data.remote.SupabaseClientProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient = SupabaseClientProvider.client

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): VibeHubDatabase =
        Room.databaseBuilder(ctx, VibeHubDatabase::class.java, "vibehub.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideUserDao(db: VibeHubDatabase): UserDao = db.userDao()
    @Provides fun providePostDao(db: VibeHubDatabase): PostDao = db.postDao()
    @Provides fun provideStoryDao(db: VibeHubDatabase): StoryDao = db.storyDao()
    @Provides fun provideConversationDao(db: VibeHubDatabase): ConversationDao = db.conversationDao()
    @Provides fun provideMessageDao(db: VibeHubDatabase): MessageDao = db.messageDao()
    @Provides fun provideNotificationDao(db: VibeHubDatabase): NotificationDao = db.notificationDao()
    @Provides fun provideHighlightDao(db: VibeHubDatabase): HighlightDao = db.highlightDao()
}
