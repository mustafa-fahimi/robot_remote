package ir.matarata.robotremote.repositories

import androidx.lifecycle.LiveData
import ir.matarata.robotremote.db.dao.RelaysDao
import ir.matarata.robotremote.models.RelaysEntity

class RelaysRepository (private val relaysDao: RelaysDao){
    val allRelaysData: LiveData<List<RelaysEntity>> = relaysDao.getRelays()

    suspend fun insertRelay(relaysEntity: RelaysEntity){
        relaysDao.insertRelay(relaysEntity)
    }

    suspend fun updateRelay(relayID: Int, newRelayTitle: String, newRelayType: String){
        relaysDao.updateRelay(relayID, newRelayTitle, newRelayType)
    }
}