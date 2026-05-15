package com.vibehub.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.vibehub.data.local.dao.*
import com.vibehub.data.local.entities.*

@Database(
    entities = [
        UserEntity::class,
        PostEntity::class,
        StoryEntity::class,
        ConversationEntity::class,
        MessageEntity::class,
        NotificationEntity::class,
        HighlightEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(StringListConverter::class)
abstract class VibeHubDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun postDao(): PostDao
    abstract fun storyDao(): StoryDao
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun notificationDao(): NotificationDao
    abstract fun highlightDao(): HighlightDao
}
