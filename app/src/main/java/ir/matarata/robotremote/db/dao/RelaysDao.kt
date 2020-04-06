package ir.matarata.robotremote.db.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import ir.matarata.robotremote.models.RelaysEntity

@Dao
interface RelaysDao {
    @Query("SELECT * FROM relaysTable")
    fun getRelays(): LiveData<List<RelaysEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRelay(relaysEntity: List<RelaysEntity>)

    @Update(entity = RelaysEntity::class, onConflict = OnConflictStrategy.IGNORE)
    suspend fun updateRelay(relaysEntity: RelaysEntity): Int
}