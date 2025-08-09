package com.pegoku.wimp

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
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
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.recyclerview.widget.RecyclerView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import com.pegoku.wimp.databinding.ShipmentInfoBinding

class ShipmentInfo : Fragment(){

    private var _binding: ShipmentInfoBinding? = null

    private lateinit var database: TrackingDatabase
    private lateinit var trackingsDao: TrackingsDao

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

        println("TrackingCode: ${arguments?.getString("trackingCode")}")

        binding.shipmentInfoBackButton.setOnClickListener {
            findNavController().navigate(R.id.action_ShipmentInfo_to_FirstFragment)
        }
    }


}