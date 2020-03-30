package ir.matarata.robotremote.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import ir.matarata.robotremote.db.dao.RelaysDao
import ir.matarata.robotremote.models.RelaysEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Database(entities = [RelaysEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase(){

    abstract fun relaysDao(): RelaysDao

    companion object{
        @Volatile
        var DB_INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope) : AppDatabase{
            val tempInstance = DB_INSTANCE
            if (tempInstance != null) {
                return tempInstance
            }
            synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "RobotRemoteDB"
                ).addCallback(AppDatabaseCallback(scope)).build()
                DB_INSTANCE = instance
                return instance
            }
        }
    }

    //This call back is for inserting data to user table only on first run of app
    private class AppDatabaseCallback(private val scope: CoroutineScope) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            DB_INSTANCE?.let { database ->
                scope.launch {
                    populateDatabase(database.relaysDao())
                }
            }
        }
        //This coroutine function insert data to db
        suspend fun populateDatabase(relaysDao: RelaysDao) {
            val tempData = listOf(RelaysEntity(1, "تفنگ تک تیر", "single"), RelaysEntity(2, "تفنگ رگبار", "multi"), RelaysEntity(3, "تفنگ ثابت", "switch"))
            relaysDao.insertRelay(tempData)
        }
    }

}