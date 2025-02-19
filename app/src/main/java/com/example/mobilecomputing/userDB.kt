package com.example.mobilecomputing

import androidx.room.*

@Entity(tableName = "user")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Int,
    @ColumnInfo(name = "user_name") val name: String?,
    @ColumnInfo(name = "profile_picture_uri") val profilePictureUri: String? = null
)

@Entity(tableName = "message")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "author") val author: String,
    @ColumnInfo(name = "body") val body: String
)

@Dao
interface UserDao {
    @Query("SELECT * FROM user")
    fun getAll(): List<User>

    @Insert
    fun insertAll(vararg users: User)

    @Delete
    fun delete(user: User)
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM message ORDER BY id ASC")
    suspend fun getAllMessages(): List<MessageEntity>

    @Insert
    suspend fun insertMessage(message: MessageEntity)

    @Insert
    suspend fun insertAll(messages: List<MessageEntity>)

    @Query("SELECT COUNT(*) FROM message")
    suspend fun getCount(): Int
}

@Database(entities = [User::class, MessageEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun messageDao(): MessageDao
}