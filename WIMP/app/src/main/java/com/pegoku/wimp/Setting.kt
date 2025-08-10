package com.pegoku.wimp

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch
import com.pegoku.wimp.databinding.SettingsBinding

class Setting : Fragment() {
    private var _binding: SettingsBinding? = null

    private val binding get() = _binding!!
    private lateinit var database: SettingsDatabase
    private lateinit var settingsDao: SettingsDao
    private lateinit var apiKey: String
    private lateinit var settings: Settings


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = SettingsBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lifecycleScope.launch {
            database = SettingsDatabase.getDatabase(requireContext())
            settingsDao = database.settingsDao()
            apiKey = settingsDao.getSettings()?.apiKey.toString()
            if (apiKey == "null") {
                binding.apiKeyText.setText("")
            } else {
                binding.apiKeyText.setText(apiKey)
            }
        }

        binding.saveButton.setOnClickListener {
            lifecycleScope.launch {
                val newApiKey = binding.apiKeyText.text.toString()
                if (newApiKey.isNotEmpty()) {

                    settings = Settings(apiKey = newApiKey)
                    settingsDao.deleteSettings() // Clear existing settings
                    settingsDao.insertSettings(settings)
                    binding.apiKeyText.setText(newApiKey)

                    findNavController().navigate(R.id.action_Settings_to_FirstFragment)

                } else {
                    binding.apiKeyText.error = "API Key cannot be empty"
                }
            }
        }
    }
}