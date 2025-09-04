package com.pegoku.wimp

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import android.view.Menu
import androidx.room.AutoMigration
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update

import com.pegoku.wimp.databinding.ActivityMainBinding
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken


@Entity(tableName = "trackings")
data class Tracking(
    @PrimaryKey(autoGenerate = true)
    val uid: Int = 0,
    @ColumnInfo(name = "tracking_number")
    val trackingNumber: String,
    @ColumnInfo(name = "courier_name")
    val courierName: String,
    @ColumnInfo(name = "courier_code")
    val courierCode: String,
    @ColumnInfo(name = "title")
    val title: String? = null,
    @ColumnInfo(name = "added_date", defaultValue = "CURRENT_TIMESTAMP")
    val addedDate: Long,
    @ColumnInfo(name = "status")
    val status: String? = "Unknown",
    @ColumnInfo(name = "events")
    val events: String? = null,
    @ColumnInfo(name = "last_updated")
    val lastUpdated: Long? = 0,
    @ColumnInfo(name = "destination_post_code")
    val destinationPostCode: String? = null,
    @ColumnInfo(name = "destination_Country_Code")
    val destinationCountryCode: String? = null,
)


@Dao
interface TrackingsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTracking(tracking: Tracking): Long

    @Query("SELECT * FROM trackings ORDER BY added_date DESC")
    suspend fun getAllTrackings(): List<Tracking>

    @Query("SELECT * FROM trackings WHERE tracking_number = :trackingNumber")
    suspend fun getTrackingByTrackingNumber(trackingNumber: String): Tracking?

    @Query("SELECT * FROM trackings WHERE uid = :uid")
    suspend fun getTrackingById(uid: Int): Tracking?

    @Query("DELETE FROM trackings WHERE uid = :uid")
    suspend fun deleteTrackingById(uid: Int)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateTracking(tracking: Tracking)

    @Query("SELECT status FROM trackings WHERE tracking_number = :trackingNumber")
    suspend fun getStatusByTrackingNumber(trackingNumber: String): String?

    @Query("UPDATE trackings SET status = :status WHERE tracking_number = :trackingNumber")
    suspend fun updateStatusByTrackingNumber(trackingNumber: String, status: String)

    @Query("UPDATE trackings SET events = :newEvents WHERE tracking_number = :trackingNumber")
    suspend fun updateEventsByTrackingNumber(trackingNumber: String, newEvents: String)

    @Query("UPDATE trackings SET events = :newEvent, status = :status, last_updated = :lastUpdated WHERE tracking_number = :trackingNumber")
    suspend fun updateStatusAndEventsByTrackingNumber(
        trackingNumber: String,
        newEvent: String,
        status: String,
        lastUpdated: Long = System.currentTimeMillis()
    )

    @Query("DELETE FROM trackings WHERE tracking_number = :trackingNumber")
    suspend fun deleteTrackingByTrackingNumber(trackingNumber: String)

    @Query("SELECT events FROM trackings WHERE tracking_number = :trackingNumber")
    suspend fun getEventsByTrackingNumber(trackingNumber: String): String?

    @Query("SELECT * FROM trackings WHERE status = :status ORDER BY added_date DESC")
    suspend fun getTrackingsByStatus(status: String): List<Tracking>

    @Query("UPDATE trackings SET tracking_number = :trackingNumber, courier_name = :courierName, courier_code = :courierCode, title = :title, destination_post_code = :destinationPostCode, destination_Country_Code = :destinationCountryCode, last_updated = :lastUpdated WHERE uid = :uid")
    suspend fun updateTrackingDetails(
        uid: Int,
        trackingNumber: String,
        courierName: String,
        courierCode: String,
        title: String?,
        lastUpdated: Long,
        destinationPostCode: String?,
        destinationCountryCode: String?,
    )

    @Query("SELECT uid FROM trackings WHERE tracking_number = :trackingNumber")
    suspend fun getIdByTrackingNumber(trackingNumber: String): Int

}

@Database(
    entities = [Tracking::class],
    version = 5,
    autoMigrations = [
        AutoMigration(from = 4, to = 5)
    ]
)
abstract class TrackingDatabase : RoomDatabase() {
    abstract fun trackingsDao(): TrackingsDao

    companion object {
        @Volatile
        private var INSTANCE: TrackingDatabase? = null

        fun getDatabase(context: Context): TrackingDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TrackingDatabase::class.java,
                    "tracking_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}


@Entity(tableName = "couriers")
data class Courier(
    @PrimaryKey(autoGenerate = true)
    val uid: Int = 0,
    @ColumnInfo(name = "courierCode")
    val courierCode: String,
    @ColumnInfo(name = "courierName")
    val courierName: String,
    @ColumnInfo(name = "website")
    val website: String? = null,
    @ColumnInfo(name = "isPost")
    val isPost: Boolean = false,
    @ColumnInfo(name = "countryCode")
    val countryCode: String? = null,
    @ColumnInfo(name = "needsDestinationPostCode")
    val needsDestinationPostCode: Boolean = false,
    @ColumnInfo(name = "needsDestinationCountryCode")
    val needsDestinationCountryCode: Boolean = false,
    @ColumnInfo(name = "isDeprecated")
    val isDeprecated: Boolean = false,
    @ColumnInfo(name = "lastUpdated")
    val lastUpdated: Long = System.currentTimeMillis(),
)

@Dao
interface CourierDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourier(courier: Courier): Long

    @Query("SELECT * FROM couriers WHERE courierCode = :courierCode")
    suspend fun checkIfCourierExists(courierCode: String): Courier?

    suspend fun addCourier(courier: Courier) {
        if (checkIfCourierExists(courier.courierCode) == null) {
            insertCourier(courier)
        }
    }

    @Query("SELECT * FROM couriers")
    suspend fun getAllCouriers(): List<Courier>

    @Query("SELECT * FROM couriers WHERE courierCode = :courierCode")
    suspend fun getCourierByCode(courierCode: String): Courier?

}

@Database(
    entities = [Courier::class],
    version = 1,
    exportSchema = true
)
abstract class CourierDatabase : RoomDatabase() {
    abstract fun courierDao(): CourierDao

    companion object {
        @Volatile
        private var INSTANCE: com.pegoku.wimp.CourierDatabase? = null

        fun getDatabase(context: Context): com.pegoku.wimp.CourierDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    com.pegoku.wimp.CourierDatabase::class.java,
                    "courier_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

@Entity
data class Settings(
    @PrimaryKey(autoGenerate = true)
    val uid: Int = 0,
    @ColumnInfo(name = "apiKey")
    val apiKey: String? = null
)

@Dao
interface SettingsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: Settings): Long

    @Query("SELECT * FROM settings LIMIT 1")
    suspend fun getSettings(): Settings?

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateSettings(settings: Settings)

    @Query("DELETE FROM settings")
    suspend fun deleteSettings()
}

@Database(
    entities = [Settings::class],
    version = 1,
    exportSchema = true
)
abstract class SettingsDatabase : RoomDatabase() {
    abstract fun settingsDao(): SettingsDao

    companion object {
        @Volatile
        private var INSTANCE: SettingsDatabase? = null
        fun getDatabase(context: Context): SettingsDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SettingsDatabase::class.java,
                    "settings_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}


fun getJsonEventsList(eventsJson: String?): List<TrackingEvent> {
    return Gson().fromJson(eventsJson ?: "[]", object : TypeToken<List<TrackingEvent>>() {}.type)
}

class MainActivity : AppCompatActivity() {


    private lateinit var appBarConfiguration: AppBarConfiguration

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

        navController.addOnDestinationChangedListener { _, _, _ ->
            // Recreate the options menu when the destination changes
            invalidateOptionsMenu()
        }

//        supportFragmentManager
//            .beginTransaction()
//            .replace(R.id.action_FirstFragment_to_ShipmentInfo, FirstFragment())
//            .commit()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
//        val navController = findNavController(R.id.nav_host_fragment_content_main)
//        val currentDestination = navController.currentDestination
//
//        if (currentDestination?.id == R.id.ShipmentInfo) {
//            menuInflater.inflate(R.menu.shipment_menu, menu)
////        menuInflater.inflate(R.menu.shipment_menu, menu)
//
//        }

        return true
    }

//    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        // Handle action bar item clicks here. The action bar will
//        // automatically handle clicks on the Home/Up addShipmentButton, so long
//        // as you specify a parent activity in AndroidManifest.xml.
//        return when (item.itemId) {
//            R.id.action_settings -> true
//            else -> super.onOptionsItemSelected(item)
//        }
//    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp() || super.onSupportNavigateUp()
    }


}