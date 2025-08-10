package com.pegoku.wimp

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.pegoku.wimp.databinding.FragmentSecondBinding

import android.widget.EditText
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import io.github.cdimascio.dotenv.dotenv


val dotenv = dotenv {
    directory = "/assets"
    filename = "env"
}


/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class SecondFragment : Fragment() {

    private var _binding: FragmentSecondBinding? = null

    public var selectedCourierName: String? = null
    private var selectedCourierCode: String? = null
    private val binding get() = _binding!!
    private var couriersList: List<Courier>? = null
    private var filteredList: List<Courier> = listOf()
    private lateinit var courierSearchText: TextInputEditText
    private lateinit var searchResultsRecyclerView: RecyclerView
    private lateinit var courierSearchEditText: EditText
    private lateinit var database: TrackingDatabase
    private lateinit var trackingsDao: TrackingsDao

    private lateinit var courierDatabase: CourierDatabase
    private lateinit var courierDao: CourierDao
    private lateinit var tracking: Tracking

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentSecondBinding.inflate(inflater, container, false)
        println(dotenv["API_KEY"])
        println(dotenv["BASE_URL"])
        return binding.root

    }


    @SuppressLint("CutPasteId")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        database = TrackingDatabase.getDatabase(requireContext())
        trackingsDao = database.trackingsDao()

        courierDatabase = CourierDatabase.getDatabase(requireContext())
        courierDao = courierDatabase.courierDao()

        lifecycleScope.launch {
            couriersList = fetchCouriers()
        }
        binding.buttonSecond.setOnClickListener {
            findNavController().navigate(R.id.action_SecondFragment_to_FirstFragment)
        }

        binding.addShipmentButton.setOnClickListener {
            saveTracking()

        }

        courierSearchEditText = view.findViewById(R.id.courierSearchEditText)
        searchResultsRecyclerView = view.findViewById(R.id.searchResultsRecyclerView)


        val searchAdapter = CourierAdapter(filteredList) { selectedCourier ->
            binding.courierSearchEditText.setText(selectedCourier.courierName)
            searchResultsRecyclerView.visibility = View.GONE
            selectedCourierName = selectedCourier.courierName
            selectedCourierCode = selectedCourier.courierCode
            courierSearchEditText.clearFocus()
            println("Selected courier: ${selectedCourier.courierName}")
            lifecycleScope.launch {
                if (courierDao.getCourierByCode(selectedCourier.courierCode)?.needsDestinationPostCode
                        ?: false
                ) {
                    binding.postalCodeFieldLayout.visibility = View.VISIBLE
                } else {
                    binding.postalCodeFieldLayout.visibility = View.GONE
                }
                if (courierDao.getCourierByCode(selectedCourier.courierCode)?.needsDestinationCountryCode
                        ?: false
                ) {
                    binding.countryCodeFieldLayout.visibility = View.VISIBLE
                } else {
                    binding.countryCodeFieldLayout.visibility = View.GONE
                }
            }


        }

        searchResultsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        searchResultsRecyclerView.adapter = searchAdapter

        courierSearchEditText.setOnFocusChangeListener { _, hasFocus ->
//            searchResultsRecyclerView.visibility = if (hasFocus) View.VISIBLE else View.GONE
            println("Search EditText focus changed: $hasFocus")
        }

        courierSearchEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {

                val query = s.toString().lowercase()
                println("Text changed: ${query}")
                filteredList =
                    couriersList?.filter { it.courierName.lowercase().contains(query) } ?: listOf()
                println(filteredList)
                // Update adapter with filtered list
                searchAdapter.updateList(filteredList)

                // Toggle RecyclerView visibility based on results
                searchResultsRecyclerView.visibility =
                    if (filteredList.isNotEmpty()) View.VISIBLE else View.GONE

            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

        })


    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    suspend fun fetchCouriers(): List<Courier>? {
        if (courierDao.getAllCouriers().isNotEmpty()) {
            return courierDao.getAllCouriers()
        } else {
            return try {
                val response = RetrofitClient.instance.getCouriers(
                    apiKey = "Bearer ${dotenv["API_KEY"]}",
                    accept = "application/json"
                )
                if (response.isSuccessful) {
                    for (courier in response.body()?.data?.couriers ?: emptyList()) {
                        if (courier == null) continue

                        if (courierDao.checkIfCourierExists(courier.courierCode) == null) {
                            val courierEntity = Courier(
                                courierCode = courier.courierCode,
                                courierName = courier.courierName,
                                website = courier.website,
                                isPost = courier.isPost,
                                countryCode = courier.countryCode,
                                needsDestinationPostCode = courier.requiredFields?.contains("destinationPostCode") == true,
                                needsDestinationCountryCode = courier.requiredFields?.contains("destinationCountryCode") == true,
                                isDeprecated = courier.isDeprecated,
                            )
                            courierDao.insertCourier(courierEntity)
                        }


                    }
                    response.body()?.data?.couriers?.map { courier ->
                        Courier(
                            courierCode = courier.courierCode,
                            courierName = courier.courierName,
                            website = courier.website,
                            isPost = courier.isPost,
                            countryCode = courier.countryCode,
                            needsDestinationPostCode = courier.requiredFields?.contains("destinationPostCode") == true,
                            needsDestinationCountryCode = courier.requiredFields?.contains("destinationCountryCode") == true,
                            isDeprecated = courier.isDeprecated
                        )
                    } ?: emptyList()
                } else {
                    null
                }
            } catch (exception: Exception) {
                println("Error fetching couriers: ${exception.message}")
                null
            } as List<Courier>?
        }
    }

    suspend fun createTracker(
        trackingNumber: String,
        courierCode: String,
        destinationPostCode: String? = null
    ): TrackerCreateResponse? {
        return try {
            val response = RetrofitClient.instance.createTracker(
                apiKey = "Bearer ${dotenv["API_KEY"]}",
                body = TrackerCreateBody(
                    trackingNumber = trackingNumber,
                    courierCode = courierCode,
                    destinationPostCode = destinationPostCode
                )
            )
            if (response.isSuccessful) {
                response.body()
            } else {
                println("Error creating tracker: ${response.errorBody()?.string()}")
                null
            }
        } catch (exception: Exception) {
            println("Error creating tracker: ${exception.message}")
        } as TrackerCreateResponse?
    }

    private fun saveTracking() {
        val trackingNumber = binding.trackingNumberText.text.toString().trim()
        val courierName = selectedCourierName ?: ""
        val title = binding.titleTextField.text.toString().trim()
        val postalCode = binding.postalCodeTextField.text.toString().trim()
        val countryCode = binding.countryCodeTextField.text.toString().trim()
        println("Tracking Number: $trackingNumber")
        println("Courier Name: $courierName")
        //        println("Courier Code: $courierCode")
        println("Title: $title")
        println("Postal code: $postalCode")
        println("Country code: $countryCode")

        lifecycleScope.launch {

            if (trackingNumber.isNotEmpty() && courierName.isNotEmpty() && if (courierDao.getCourierByCode(
                        getCourierCodeByName(courierName)
                    )?.needsDestinationPostCode == true
                ) postalCode.isNotEmpty() else true && if (courierDao.getCourierByCode(
                        getCourierCodeByName(
                            courierName
                        )
                    )?.needsDestinationCountryCode == true
                ) countryCode.isNotEmpty() && countryCode.length == 2 else true
            ) {
                try {
                    Snackbar.make(
                        binding.root,
                        "Tracking saved successfully",
                        Snackbar.LENGTH_SHORT
                    ).show()

                    // create tracker for Ship24
                    createTracker(trackingNumber, getCourierCodeByName(courierName))
                        .let { response ->
                            if (response != null) {
                                println("Tracker created successfully: $response")
                                if (trackingsDao.getTrackingByNumber(trackingNumber) == null) {
                                    tracking = Tracking(
                                        trackingNumber = trackingNumber,
                                        courierName = courierName,
                                        courierCode = getCourierCodeByName(courierName),
                                        title = title,
                                        addedDate = System.currentTimeMillis(),
                                        lastUpdated = 0 // So it will be fetched immediately
                                    )
                                    trackingsDao.insertTracking(tracking)
                                } else {
                                    Snackbar.make(
                                        binding.root,
                                        "Tracking number already exists in the database",
                                        Snackbar.LENGTH_SHORT
                                    ).show()
                                    println("Tracking number already exists in the database")
                                }
                            } else {
                                Snackbar.make(
                                    binding.root,
                                    "Failed to create tracker",
                                    Snackbar.LENGTH_SHORT
                                ).show()
                            }
                        }

                    clearFields()
                } catch (e: Exception) {
                    Snackbar.make(
                        binding.root,
                        "Error saving tracking: ${e.message}",
                        Snackbar.LENGTH_SHORT
                    ).show()
                    println("Error saving tracking: ${e.message}")
                }
            } else {
                Snackbar.make(
                    binding.root,
                    "Please fill in all the fields",
                    Snackbar.LENGTH_SHORT
                )
                    .show()
            }
        }
    }

    private fun getCourierCodeByName(courierName: String): String {
        return couriersList?.find { it.courierName == courierName }?.courierCode ?: ""
    }

    private fun clearFields() {
        binding.trackingNumberText.setText("")
        binding.courierSearchEditText.setText("")
        binding.titleTextField.setText("")
        selectedCourierName = null
        selectedCourierCode = null
        findNavController().navigate(R.id.action_SecondFragment_to_FirstFragment)
    }

}

class CourierAdapter(
    private var couriers: List<Courier>,
    private val onItemClick: (Courier) -> Unit

) :
    RecyclerView.Adapter<CourierAdapter.CourierViewHolder>() {


    inner class CourierViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(R.id.textViewCourier)

        init {
            view.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(couriers[position])

                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CourierViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_courier, parent, false)
        return CourierViewHolder(view)
    }

    override fun onBindViewHolder(holder: CourierViewHolder, position: Int) {
        val courier = couriers[position]
        //        SecondFragment.selectedCourierName = courier.courierName
        holder.textView.text = courier.courierName
        holder.itemView.setOnClickListener {
            onItemClick(courier)
        }
        //        holder.textView.text = couriers[position].courierName
    }

    override fun getItemCount(): Int = couriers.size

    fun updateList(newList: List<Courier>) {
        couriers = newList
        notifyDataSetChanged()
    }
}