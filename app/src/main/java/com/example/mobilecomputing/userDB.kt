package com.example.mobilecomputing

import androidx.room.*

@Entity(tableName = "user")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Int,
    @ColumnInfo(name = "user_name") val name: String?,
    @ColumnInfo(name = "profile_picture_uri") val profilePictureUri: String? = null

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

@Database(entities = [User::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
}