package com.pegoku.wimp

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import com.google.android.material.snackbar.Snackbar
import com.pegoku.wimp.databinding.ActivityMainBinding
import com.pegoku.wimp.databinding.FragmentSecondBinding

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import okhttp3.OkHttpClient
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.logging.HttpLoggingInterceptor
import io.github.cdimascio.dotenv.dotenv
import kotlin.text.get

val dotenv = dotenv {
    directory = "/assets"
    filename = "env"
}


data class TrackingRequestBody(
    val trackingNumber: String,
    val courierCode: String? = null,
    val destinationPostCode: String? = null
)

data class CouriersResponse(
    val data: CouriersData
)

data class CouriersData(
    val couriers: List<Couriers>
)

data class Couriers(
    val courierCode: String,
    val courierName: String,
    val website: String,
    val isPost: Boolean,
    val countryCode: String,
    val requiredFields: List<String>,
    val isDeprecated: Boolean
)

interface Ship24ApiService {
//    @POST("tracking/track")
//    suspend fun track(
//        @Header("Bearer") apiKey: String,
//        @Body body: TrackingRequestBody
//    ): Response<TrackingResponse>

    @GET("couriers")
    suspend fun getCouriers(
        @Header("Authorization") apiKey: String,
        @Header("Accept") accept: String = "application/json",
    ): Response<CouriersResponse>

}

object RetrofitClient {
    //Retrofit client
    private val BASE_URL: String by lazy { dotenv["BASE_URL"] ?: "" }

    // logging of network requests and responses
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY // BASIC, HEADERS
    }
    private val client = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()

    val instance: Ship24ApiService by lazy {

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(Ship24ApiService::class.java)
    }

}


/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class SecondFragment : Fragment() {

    private var _binding: FragmentSecondBinding? = null

    public var selectedCourierName: String ? = null
    private var selectedCourierCode: String ? = null
    private val binding get() = _binding!!
    private var couriersList: List<Couriers>? = null
    private var filteredList: List<Couriers> = listOf()
    private lateinit var courierSearchText: TextInputEditText
    private lateinit var searchResultsRecyclerView: RecyclerView
    private lateinit var courierSearchEditText: EditText
    private lateinit var database: TrackingDatabase
    private lateinit var trackingsDao: TrackingsDao


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

    suspend fun fetchCouriers(): List<Couriers>? {
        return try {
            val response = RetrofitClient.instance.getCouriers(
                apiKey = "Bearer ${dotenv["API_KEY"]}",
                accept = "application/json"
            )
            if (response.isSuccessful) {
                response.body()?.data?.couriers
            } else {
                null
            }
        } catch (exception: Exception) {
            println("Error fetching couriers: ${exception.message}")
            null
        }
    }


    private fun saveTracking() {
        val trackinNumbeer = binding.trackingNumberText.text.toString().trim()
        val courierName = selectedCourierName ?: ""
        val title = binding.titleTextField.text.toString().trim() ?: trackinNumbeer

        println("Tracking Number: $trackinNumbeer")
        println("Courier Name: $courierName")
//        println("Courier Code: $courierCode")
        println("Title: $title")

        if (trackinNumbeer.isNotEmpty() && courierName.isNotEmpty()) {
            val tracking = Tracking(
                trackingNumber = trackinNumbeer,
                courierName = courierName,
                courierCode = getCourierCodeByName(courierName),
                title = title,
                addedDate = System.currentTimeMillis()
            )

            lifecycleScope.launch {
                try {
                    trackingsDao.insertTracking(tracking)
                    Snackbar.make(
                        binding.root,
                        "Tracking saved successfully",
                        Snackbar.LENGTH_SHORT
                    ).show()
                    clearFields()
                } catch (e: Exception) {
                    Snackbar.make(
                        binding.root,
                        "Error saving tracking: ${e.message}",
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
            }
        } else {
            Snackbar.make(binding.root, "Please fill in all the fields", Snackbar.LENGTH_SHORT)
                .show()
        }
    }

    private fun getCourierCodeByName(courierName: String): String {
        return couriersList?.find {it.courierName == courierName }?.courierCode ?: ""
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
    private var couriers: List<Couriers>,
    private val onItemClick: (Couriers) -> Unit

) :
    RecyclerView.Adapter<CourierAdapter.CourierViewHolder>() {


    inner class CourierViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(R.id.textViewCourier)
        init{
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

    fun updateList(newList: List<Couriers>) {
        couriers = newList
        notifyDataSetChanged()
    }
}