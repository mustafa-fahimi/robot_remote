package ir.matarata.robotremote.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import ir.matarata.robotremote.db.AppDatabase
import ir.matarata.robotremote.models.RelaysEntity
import ir.matarata.robotremote.repositories.RelaysRepository
import kotlinx.coroutines.launch

class RelaysVM(application: Application) : AndroidViewModel(application){

    private val relaysRepository: RelaysRepository
    val allRelaysData: LiveData<List<RelaysEntity>>

    init {
        val relaysDao = AppDatabase.getDatabase(application, viewModelScope).relaysDao()
        relaysRepository = RelaysRepository(relaysDao)
        allRelaysData = relaysRepository.allRelaysData
    }

    fun insertRelay(relaysEntity: RelaysEntity) = viewModelScope.launch {
        relaysRepository.insertRelay(relaysEntity)
    }

    fun updateRelay(relayID: Int, newRelayTitle: String, newRelayType: String) = viewModelScope.launch {
        relaysRepository.updateRelay(relayID, newRelayTitle, newRelayType)
    }
}