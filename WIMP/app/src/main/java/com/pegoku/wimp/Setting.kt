package com.pegoku.wimp

import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.textfield.TextInputLayout
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

        val apiKeyLayout = view.findViewById<TextInputLayout>(R.id.apiKeyLayout)
        var helperTextString = "For more information, visit <a href=\"https://www.ship24.com/tracking-api\">Ship24 API</a>"

        apiKeyLayout.helperText = Html.fromHtml(helperTextString, Html.FROM_HTML_MODE_LEGACY)
        var helperTextView = apiKeyLayout.findViewById<TextView>(com.google.android.material.R.id.textinput_helper_text)
        helperTextView?.movementMethod = LinkMovementMethod.getInstance()

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