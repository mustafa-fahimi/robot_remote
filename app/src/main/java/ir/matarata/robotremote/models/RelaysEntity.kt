package ir.matarata.robotremote.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "relaysTable")
data class RelaysEntity(
    @PrimaryKey(autoGenerate = false)
    var relayId: Int,
    var relayTitle: String,
    var relayType: String
)
