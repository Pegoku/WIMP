package com.pegoku.wimp

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import android.widget.TextView
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.recyclerview.widget.RecyclerView
import com.pegoku.wimp.databinding.ShipmentInfoBinding

class ShipmentInfo : Fragment() {

    private var _binding: ShipmentInfoBinding? = null

    private lateinit var database: TrackingDatabase
    private lateinit var trackingsDao: TrackingsDao

    private lateinit var shipmentEventAdapter: ShipmentEventAdapter
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = ShipmentInfoBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        database = TrackingDatabase.getDatabase(requireContext())
        trackingsDao = database.trackingsDao()

        binding.eventListRecyclerview.layoutManager = LinearLayoutManager(requireContext())


        println("TrackingCode: ${arguments?.getString("trackingCode")}")

        binding.shipmentInfoBackButton.setOnClickListener {
            findNavController().navigate(R.id.action_ShipmentInfo_to_FirstFragment)
        }
        loadEvents(arguments?.getString("trackingCode") ?: "")

        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.shipment_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_edit -> {
                        findNavController().navigate(
                            R.id.action_ShipmentInfo_to_SecondFragment,
                            Bundle().apply {
                                putString("trackingCode", arguments?.getString("trackingCode"))
                            })
                        true
                    }

                    R.id.action_delete -> {
                        lifecycleScope.launch {
                            println("Deleting ${arguments?.getString("trackingCode")}")
                            trackingsDao.deleteTrackingByTrackingNumber(
                                arguments?.getString("trackingCode") ?: ""
                            )
                        }
                        findNavController().navigate(R.id.action_ShipmentInfo_to_FirstFragment)
                        true
                    }

                    R.id.action_mark_delivered -> {
                        lifecycleScope.launch {
                            println(
                                "Marking as delivered for tracking code: ${
                                    arguments?.getString(
                                        "trackingCode"
                                    )
                                }"
                            )
                            trackingsDao.updateStatusByTrackingNumber(
                                arguments?.getString("trackingCode") ?: "",
                                "delivered"
                            )
                        }
                        findNavController().navigate(R.id.action_ShipmentInfo_to_FirstFragment)
                        true
                    }

                    else -> false
                }
            }
        }, viewLifecycleOwner) // , Lifecycle.State.RESUMED)

    }


    private fun loadEvents(trackingCode: String) {
        lifecycleScope.launch {
            println("Loading events for tracking code: $trackingCode")
            shipmentEventAdapter = ShipmentEventAdapter(emptyList())
            binding.eventListRecyclerview.adapter = shipmentEventAdapter
            binding.eventListRecyclerview.visibility = View.GONE
            binding.shipmentInfoNoAvailableText.visibility = View.GONE
            if (_binding != null) {
                try {
                    val eventsStr = trackingsDao.getEventsByTrackingNumber(trackingCode)
                    print("Events String: $eventsStr")
                    if (eventsStr.isNullOrEmpty() || eventsStr == "[]") {
//                        Snackbar.make(
//                            binding.root,
//                            "No events found for tracking number: $trackingCode",
//                            Snackbar.LENGTH_LONG
//                        ).show()
                        println("No events found for tracking number: $trackingCode")
                        binding.shipmentInfoNoAvailableText.visibility = View.VISIBLE
                    } else {
                        val eventList = getJsonEventsList(eventsStr)
                        shipmentEventAdapter.updateList(eventList)
                        println("Loaded ${eventList.size} events for tracking nubber: $trackingCode")
                        println("Events List: $eventList")
                        binding.eventListRecyclerview.visibility = View.VISIBLE
                    }
                } catch (
                    e: Exception
                ) {
                    Snackbar.make(
                        binding.root,
                        "Error loading events: ${e.message}",
                        Snackbar.LENGTH_LONG
                    ).show()
                    println("Error loading events: ${e.message}")
                }

            }
        }
    }

}


class ShipmentEventAdapter(private var events: List<TrackingEvent>) :
    RecyclerView.Adapter<ShipmentEventAdapter.EventViewHolder>() {

    inner class EventViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val message: TextView = itemView.findViewById(R.id.eventMessageText)
        val date: TextView = itemView.findViewById(R.id.eventDateText)
        val courierName: TextView = itemView.findViewById(R.id.eventCourierNameText)
        val status: TextView = itemView.findViewById(R.id.eventStatusText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.tracking_event, parent, false)
        return EventViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val event = events[position]
        holder.message.text = event.status
//        holder.date.text = SimpleDateFormat(
//            "yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        try {
            val outputFormat = SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault())
            val formats = listOf(
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()),
                SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS'Z'", Locale.getDefault()),
                SimpleDateFormat("yyyy-MM-dd HH:mm:ssXXX", Locale.getDefault())
            )
            var parsedDate: Date? = null
            for (format in formats) {
                try {
                    parsedDate = format.parse(event.occurrenceDatetime)
                    break
                } catch (e: Exception) {
                    // Continue to the next format if parsing fails
                    continue
                }
            }
            holder.date.text =
                if (parsedDate != null) outputFormat.format(parsedDate) else event.occurrenceDatetime
        } catch (e: Exception) {
            holder.date.text = event.occurrenceDatetime // Fallback if parsing fails
        }
        holder.courierName.text = event.courierCode
        holder.status.text = event.statusMilestone
    }

    override fun getItemCount(): Int = events.size

    fun updateList(newList: List<TrackingEvent>) {
        events = newList
        notifyDataSetChanged()
    }
}