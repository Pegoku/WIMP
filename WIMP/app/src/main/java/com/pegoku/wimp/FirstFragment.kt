package com.pegoku.wimp

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.pegoku.wimp.databinding.FragmentFirstBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import com.google.android.material.card.MaterialCardView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.view.MenuProvider
import androidx.core.view.doOnPreDraw
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.Priority
import com.google.android.material.transition.MaterialSharedAxis
import java.util.Objects.toString
import kotlin.math.abs
import com.google.gson.Gson
import kotlinx.coroutines.channels.Channel

class FirstFragment : Fragment() {



    private var _binding: FragmentFirstBinding? = null

    private lateinit var database: TrackingDatabase
    private lateinit var trackingsDao: TrackingsDao

    private lateinit var trackingAdapter: TrackingAdapter

    private lateinit var apiKey: String

    private val binding get() = _binding!!


    companion object {
        private const val CHANNEL_ID = "statusUpdates"
        private const val NOTIFICATION_ID = 1
    }

    private val requestPostNotifications =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                println("Notification permission granted")
                sendTestNotification()
            } else {
                println("Notification permission denied")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        postponeEnterTransition()

        exitTransition = MaterialSharedAxis(MaterialSharedAxis.X, true)
        reenterTransition = MaterialSharedAxis(MaterialSharedAxis.X, false)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Create the NotificationChannel.
//        val name = "Status Updates"
//        val descriptionText = "Notifications for tracking status updates"
//        val importance = NotificationManager.IMPORTANCE_DEFAULT
//        val CHANNEL_ID = "statusUpdates"
//        val mChannel = NotificationChannel(CHANNEL_ID, name, importance)
//        mChannel.description = descriptionText
//        // Register the channel with the system. You can't change the importance
//        // or other notification behaviors after this.
//        val notificationManager = requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//        notificationManager.createNotificationChannel(mChannel)

        lifecycleScope.launch {
            apiKey = RetrofitClient.getApiKey(requireContext())
            if (apiKey.isEmpty() || apiKey == "null") {

                findNavController().navigate(R.id.action_FirstFragment_to_Settings)
                Snackbar.make(
                    binding.root,
                    "No API key configured. Please set your API key in settings.",
                    Snackbar.LENGTH_LONG
                ).show()

                return@launch
            }

        }


        database = TrackingDatabase.getDatabase(requireContext())
        trackingsDao = database.trackingsDao()

        setupRecyclerView()

        displayTrackings()

        // Wait until the RecyclerView is laid out before starting the transition






        updateTrackings()
        displayTrackings()

//        if (binding.bottomNavigation.selectedItemId == R.id.bottom_nav_all) {
//            loadTrackings()
//        }
        binding.buttonFirst.setOnClickListener {
            // Notification test
//            if(checkNotificationPermission()) {
//                sendTestNotification()
//            }
            findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
        }


        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.start_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_settings -> {
                        findNavController().navigate(
                            R.id.action_FirstFragment_to_Settings,
                        )
                        true
                    }

                    else -> false
                }
            }
        }, viewLifecycleOwner) // , Lifecycle.State.RESUMED)


    }

    private fun displayTrackings(){
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.bottom_nav_all -> {
                    loadTrackings()
                    true
                }

                R.id.bottom_nav_waiting -> {
                    loadTrackings(
                        listOf("pending", "exception", "failed_attempt")
                    )
                    true
                }

                R.id.bottom_nav_delivered -> {
                    loadTrackings(
                        listOf("delivered", "available_for_pickup")
                    )
                    true
                }

                R.id.bottom_nav_shipped -> {
                    loadTrackings(
                        listOf("in_transit", "out_for_delivery", "info_received")
                    )
                    true
                }

                else -> false

            }
        }
    }

    private fun setupRecyclerView() {
        trackingAdapter = TrackingAdapter(emptyList()) { tracking ->
//            Snackbar.make(
//                binding.root,
//                "Selected: ${tracking.trackingNumber}",
//                Snackbar.LENGTH_SHORT
//            ).show()
            findNavController().navigate(R.id.action_FirstFragment_to_ShipmentInfo, Bundle().apply {
                putString("trackingCode", tracking.trackingNumber)
            })
        }

        binding.trackingListRecyclerview.layoutManager = LinearLayoutManager(requireContext())
        binding.trackingListRecyclerview.adapter = trackingAdapter
//        binding.trackingListRecyclerview.apply {
//            layoutManager = LinearLayoutManager(requireContext())
//            adapter = trackingAdapter
//        }
    }

    private fun loadTrackings(status: List<String>? = null) {
        lifecycleScope.launch {
            if (_binding != null) {
                try {
                    var trackings: MutableList<Tracking> = mutableListOf()
                    var listTrackings: List<Tracking>
                    if (status != null) {
                        for (s in status) {
                            listTrackings = trackingsDao.getTrackingsByStatus(s)
                            for (t in listTrackings) {
                                trackings.add(t)
                            }
                        }
                    } else {
                        trackings = trackingsDao.getAllTrackings().toMutableList()
                    }

                    if (trackings.isEmpty()) {
                        binding.emptyListText.visibility = View.VISIBLE
                        binding.trackingListRecyclerview.visibility = View.GONE
                    } else {
                        binding.emptyListText.visibility = View.GONE
                        binding.trackingListRecyclerview.visibility = View.VISIBLE
                        trackingAdapter.updateList(trackings)
                    }
                } catch (e: Exception) {
                    Snackbar.make(
                        binding.root,
                        "Error loading trackings: ${e.message}",
                        Snackbar.LENGTH_LONG
                    ).show()
                    println("Error loading trackings: ${e.message}")

                }
            }
                startPostponedEnterTransition()

        }

    }

    private fun eventsToJson(events: List<TrackingEvent>?): String {
        return Gson().toJson(events)
    }

//    private fun parseHttpResponse(json: String): List<TrackingResponse> {
//        val gson = Gson()
//        val response = gson.fromJson(json, TrackingResponse)
//        return response
//    }

    private fun updateTrackings() {
        lifecycleScope.launch {
            try {
                if (!::apiKey.isInitialized) {
                    apiKey = RetrofitClient.getApiKey(requireContext())
                }
                if (apiKey.isEmpty()) {
                    Snackbar.make(
                        binding.root,
                        "No API key configured. Please set your API key in settings.",
                        Snackbar.LENGTH_LONG
                    ).show()
//                    findNavController().navigate(R.id.action_FirstFragment_to_Settings)
                    return@launch
                }

                val trackings = trackingsDao.getAllTrackings()
                for (tracking in trackings) {
                    if ((tracking.lastUpdated == null || (tracking.lastUpdated < System.currentTimeMillis() - 5 * 60 * 1000L)) && tracking.status != "delivered") {

                        val response = RetrofitClient.instance.track(
                            trackingNumber = tracking.trackingNumber,
                            apiKey = "Bearer $apiKey",
                        )
                        if (response.isSuccessful) {
                            val status = response.body()
                            if (status == null || status.data.trackings.isEmpty()) {
                                println("No tracking data found for ${tracking.trackingNumber}")
                                continue
                            }
                            val trackingData = status.data.trackings.firstOrNull()
                            println("Data for ${tracking.trackingNumber}: $trackingData")
                            val jsonEvents = eventsToJson(trackingData?.events)
                            println("Events for ${tracking.trackingNumber}: $jsonEvents")

                            trackingsDao.updateStatusAndEventsByTrackingNumber(
                                trackingNumber = tracking.trackingNumber,
                                newEvent = jsonEvents,
                                status = trackingData?.shipment?.statusMilestone ?: "Unknown",
                                lastUpdated = System.currentTimeMillis()
                            )
                        } else {
                            val errorBody = response.errorBody()?.string() ?: "Unknown error"
                            if (errorBody.contains("tracker_not_found") == true
                            ) {
                                println("Tracking number ${tracking.trackingNumber} not found, removing from database.")
                                trackingsDao.deleteTrackingByTrackingNumber(tracking.trackingNumber)
                            } else
                                println(
                                    "Failed to update status for ${tracking.trackingNumber}: ${
                                        errorBody ?: "Unknown error"
                                    }"
                                )
                            if (errorBody.contains("auth_invalid_api_key") == true) {
                                Snackbar.make(
                                    binding.root,
                                    "Invalid API key. Please check your settings.",
                                    Snackbar.LENGTH_LONG
                                ).show()
                            }
                        }

                    } else {
                        println(
                            "Skipping update for ${tracking.trackingNumber}, last updated ${
                                abs(
                                    tracking.lastUpdated?.minus(System.currentTimeMillis()) ?: 0L
                                ) / 1000
                            }s ago and ${tracking.status}."
                        )
                    }
                }
//                loadTrackings() // Refresh the list after updating statuses
            } catch (e: Exception) {
                Snackbar.make(
                    binding.root,
                    "Error updating tracking: ${e.message}",
                    Snackbar.LENGTH_LONG
                ).show()
                println("Error updating tracking: ${e.message}")
            }
        }

    }

    private fun checkNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        val granted = ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            requestPostNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        return granted
    }

    private fun sendTestNotification() {
        val notification = NotificationCompat.Builder(requireContext(), CHANNEL_ID)
            .setSmallIcon(R.drawable.package_variant_closed_check)
            .setContentTitle("TestNotification")
            .setContentText("Test Notification Content")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(requireContext()).notify(NOTIFICATION_ID, notification)
    }

    override fun onResume() {
        super.onResume()
        loadTrackings() // Refresh the list when the fragment is resumed
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

//    suspend fun updateTracking(trackingNumber: String){
//        try {
//            val response: RetrofitClient.instance.track()
//        }
//
//    }
}

class TrackingAdapter(
    private var trackings: List<Tracking>,
    private val onItemClick: (Tracking) -> Unit
) : RecyclerView.Adapter<TrackingAdapter.TrackingViewHolder>() {

    inner class TrackingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titleText: TextView = view.findViewById(R.id.titleText)
        val trackingNumberText: TextView = view.findViewById(R.id.trackingNumberText)
        val courierNameText: TextView = view.findViewById(R.id.courierNameText)
        val dateText: TextView = view.findViewById(R.id.dateText)
        val statusText: TextView = view.findViewById(R.id.statusText)
        val cardView: MaterialCardView = view.findViewById(R.id.trackingCard)

        init {
            view.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(trackings[position])
//                    Snackbar.make(view, "Selected: ${trackings[position].trackingNumber}", Snackbar.LENGTH_SHORT).show()
                    println("Selected: ${trackings[position].trackingNumber}")
//                    findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackingViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_tracking, parent, false)
        return TrackingViewHolder(view)
    }

    override fun onBindViewHolder(holder: TrackingViewHolder, position: Int) {
        val tracking = trackings[position]
        holder.titleText.text = tracking.title ?: tracking.trackingNumber
        holder.trackingNumberText.text = tracking.trackingNumber
        holder.courierNameText.text = tracking.courierName
        holder.dateText.text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            .format(Date(tracking.addedDate))
        holder.statusText.text =
            when (tracking.status) {
                "pending" -> "Waiting for data"
                "in_transit" -> "In transit"
                "delivered" -> "Delivered"
                "available_for_pickup" -> "Available for pickup"
                "exception" -> "Exception occurred"
                "failed_attempt" -> "Failed delivery attempt"
                "out_for_delivery" -> "Out for delivery"
                "info_received" -> "Info received"
                else -> "Unknown status"
            }

    }

    override fun getItemCount(): Int = trackings.size

    fun updateList(newList: List<Tracking>) {
        trackings = newList
        notifyDataSetChanged()
    }
}