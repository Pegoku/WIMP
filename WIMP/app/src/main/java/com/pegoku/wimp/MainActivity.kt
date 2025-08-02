package com.pegoku.wimp

import android.content.Context
import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import android.view.Menu
import android.view.MenuItem
import androidx.collection.mutableIntIntMapOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update

import com.pegoku.wimp.databinding.ActivityMainBinding
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

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
    val addedDate: Long
)

@Dao
interface TrackingsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTracking(tracking: Tracking): Long

    @Query("SELECT * FROM trackings ORDER BY added_date DESC")
    suspend fun getAllTrackings(): List<Tracking>

    @Query("SELECT * FROM trackings WHERE tracking_number = :trackingNumber")
    suspend fun getTrackingByNumber(trackingNumber: String): Tracking?

    @Query("DELETE FROM trackings WHERE tracking_number = :trackingNumber")
    suspend fun deleteTrackingByNumber(trackingNumber: String)

    @Query("SELECT * FROM trackings WHERE uid = :uid")
    suspend fun getTrackingById(uid: Int): Tracking?

    @Query("DELETE FROM trackings WHERE uid = :uid")
    suspend fun deleteTrackingById(uid: Int)

}

@Database(
    entities = [Tracking::class],
    version = 1,
    exportSchema = false
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


    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up addShipmentButton, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }


}