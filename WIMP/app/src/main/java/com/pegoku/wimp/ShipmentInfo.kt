package com.pegoku.wimp

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
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
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            val outputFormat = SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault())
            val date = inputFormat.parse(event.occurrenceDatetime)
            holder.date.text =
                if (date != null) outputFormat.format(date) else event.occurrenceDatetime
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