package com.pegoku.wimp

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
import android.widget.Button // Import Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView // Import TextView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.logging.HttpLoggingInterceptor
import io.github.cdimascio.dotenv.dotenv
import kotlin.text.get

val dotenv = dotenv {
    directory = "/assets"
    filename = "env" // instead of '.env', use 'env'
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
    // Initialize Retrofit client here
    private val BASE_URL: String by lazy { dotenv["BASE_URL"] ?: "" }

    // Optional: For logging network requests and responses
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY // Level.BASIC, Level.HEADERS
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
    // This property is only valid between onCreateView and
    // onDestroyView.
    private var selectedCourierName: String? = null
    private val binding get() = _binding!!
    private var couriersList: List<Couriers>? = null
    private var filteredList: List<Couriers> = listOf()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentSecondBinding.inflate(inflater, container, false)
        println(dotenv["API_KEY"])
        println(dotenv["BASE_URL"])
        return binding.root

    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lifecycleScope.launch {
            couriersList = fetchCouriers()
        }
        binding.buttonSecond.setOnClickListener {
            findNavController().navigate(R.id.action_SecondFragment_to_FirstFragment)
        }
        binding.courierSearchEditText.setOnClickListener {

            showFilterableSpinner()

        }

        courierSearchText = bindingcourierSearchEditText.text.toString()


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

    private fun showFilterableSpinner() {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_spinner)

        val recyclerView = dialog.findViewById<RecyclerView>(R.id.recyclerView)
        val editText = dialog.findViewById<EditText>(R.id.editTextFilter)

        filteredList = couriersList?.take(10) ?: listOf() // Initially show the first 10 results
        val adapter = CourierAdapter(filteredList) { selectedCourier ->
            // Handle courier selection
            selectedCourierName = selectedCourier.courierName
            Snackbar.make(
                requireView(),
                "Selected Courier: ${selectedCourier.courierName}",
                Snackbar.LENGTH_SHORT
            ).show()
            binding.courierSearchEditText.setText(selectedCourier.courierName)
            dialog.dismiss()
        }
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        editText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().lowercase()
                filteredList =
                    couriersList?.filter { it.courierName.lowercase().contains(query) }?.take(10)
                        ?: listOf()
                adapter.updateList(filteredList)
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        dialog.show()
    }
}

class CourierAdapter(
    private var couriers: List<Couriers>,
    private val onItemClick: (Couriers) -> Unit
) :
    RecyclerView.Adapter<CourierAdapter.CourierViewHolder>() {


    inner class CourierViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(R.id.textViewCourier)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CourierViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_courier, parent, false)
        return CourierViewHolder(view)
    }

    override fun onBindViewHolder(holder: CourierViewHolder, position: Int) {
        val courier = couriers[position]
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