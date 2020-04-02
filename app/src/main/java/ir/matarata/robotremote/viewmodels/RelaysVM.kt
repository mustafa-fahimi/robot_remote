package ir.matarata.robotremote.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import ir.matarata.robotremote.db.AppDatabase
import ir.matarata.robotremote.models.RelaysEntity
import ir.matarata.robotremote.repositories.RelaysRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class RelaysVM(application: Application) : AndroidViewModel(application){

    private val relaysRepository: RelaysRepository
    val allRelaysData: LiveData<List<RelaysEntity>>

    init {
        val relaysDao = AppDatabase.getDatabase(application, viewModelScope).relaysDao()
        relaysRepository = RelaysRepository(relaysDao)
        allRelaysData = relaysRepository.allRelaysData
    }

    fun updateRelay(relaysEntity: RelaysEntity) = viewModelScope.launch {
        delay(1500)
        relaysRepository.updateRelay(relaysEntity)
    }
}