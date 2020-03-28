package ir.matarata.robotremote.db.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import ir.matarata.robotremote.models.RelaysEntity

@Dao
interface RelaysDao {
    @Query("SELECT * FROM relaysTable")
    fun getRelays(): LiveData<List<RelaysEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRelay(relaysEntity: RelaysEntity)

    @Query("UPDATE relaysTable SET relayTitle = :newRelayTitle AND relayType = :newRelayType WHERE relayId = :relayID")
    suspend fun updateRelay(relayID: Int, newRelayTitle: String, newRelayType: String)
}