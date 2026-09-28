package com.example.autoutil.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.ArrayAdapter
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.autoutil.databinding.ActivityMainBinding
import com.example.autoutil.engine.WorkflowStatus
import com.example.autoutil.feedback.BatteryHelper
import com.example.autoutil.service.AutomationForegroundService

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel
    private lateinit var stepAdapter: StepAdapter
    private lateinit var logAdapter: LogAdapter
    private lateinit var batteryHelper: BatteryHelper

    private var profileNames = mutableListOf<String>()

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        batteryHelper = BatteryHelper(this)

        setupSteps()
        setupLog()
        setupProfileSpinner()
        setupButtons()
        observeViewModel()
        requestNotificationPermissionIfNeeded()
    }

    override fun onResume() {
        super.onResume()
        viewModel.reload()
    }

    private fun setupSteps() {
        stepAdapter = StepAdapter(
            onEdit = { step ->
                StepEditDialog.show(this, step) { updated -> viewModel.updateStep(updated) }
            },
            onDelete = { step -> viewModel.deleteStep(step.id) }
        )
        binding.recyclerSteps.layoutManager = LinearLayoutManager(this)
        binding.recyclerSteps.adapter = stepAdapter
    }

    private fun setupLog() {
        logAdapter = LogAdapter(com.example.autoutil.engine.AutomationController.logRepository)
        binding.recyclerLog.layoutManager = LinearLayoutManager(this)
        binding.recyclerLog.adapter = logAdapter
    }

    private fun setupProfileSpinner() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, profileNames)
        binding.spinnerProfiles.adapter = adapter

        binding.spinnerProfiles.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: android.widget.AdapterView<*>?,
                view: android.view.View?,
                position: Int,
                id: Long
            ) {
                viewModel.profiles.value?.getOrNull(position)?.let { viewModel.selectProfile(it) }
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
        }
    }

    private fun setupButtons() {
        binding.btnNewProfile.setOnClickListener { promptNewProfileName() }
        binding.btnSaveProfile.setOnClickListener {
            viewModel.saveActiveProfile()
        }
        binding.btnAddStep.setOnClickListener {
            if (viewModel.activeProfile.value == null) {
                promptNewProfileName()
            } else {
                StepEditDialog.show(this, null) { step -> viewModel.addStep(step) }
            }
        }
        binding.btnStart.setOnClickListener {
            if (!viewModel.isAccessibilityReady()) {
                openAccessibilitySettings()
                return@setOnClickListener
            }
            val userText = binding.editUserText.text.toString()
            startForegroundService(Intent(this, AutomationForegroundService::class.java))
            viewModel.startWorkflow(userText)
        }
        binding.btnStop.setOnClickListener { viewModel.stopWorkflow() }
        binding.btnOpenAccessibilitySettings.setOnClickListener { openAccessibilitySettings() }
        binding.btnBatterySettings.setOnClickListener { batteryHelper.openBatterySettings() }
    }

    private fun observeViewModel() {
        viewModel.profiles.observe(this) { profiles ->
            profileNames.clear()
            profileNames.addAll(profiles.map { it.name })
            (binding.spinnerProfiles.adapter as ArrayAdapter<*>).let {
                (it as ArrayAdapter<String>).notifyDataSetChanged()
            }
            val active = viewModel.activeProfile.value
            val index = profiles.indexOfFirst { it.id == active?.id }
            if (index >= 0) binding.spinnerProfiles.setSelection(index)
        }

        viewModel.activeProfile.observe(this) { profile ->
            stepAdapter.submitList(profile?.steps ?: emptyList())
        }

        viewModel.logEntries.observe(this) { entries ->
            logAdapter.submitList(entries)
            binding.recyclerLog.scrollToPosition(maxOf(0, entries.size - 1))
        }

        viewModel.workflowStatus.observe(this) { status ->
            binding.txtCurrentStatus.text = statusLabel(status)
        }

        viewModel.currentStep.observe(this) { (index, count) ->
            binding.txtCurrentStep.text = if (index >= 0 && count > 0) {
                "Step ${index + 1} / $count"
            } else {
                getString(com.example.autoutil.R.string.status_idle)
            }
        }
    }

    private fun statusLabel(status: WorkflowStatus): String = when (status) {
        WorkflowStatus.IDLE -> getString(com.example.autoutil.R.string.status_idle)
        WorkflowStatus.RUNNING -> getString(com.example.autoutil.R.string.status_running)
        WorkflowStatus.COMPLETED -> getString(com.example.autoutil.R.string.status_completed)
        WorkflowStatus.FAILED -> getString(com.example.autoutil.R.string.status_failed)
        WorkflowStatus.STOPPED -> getString(com.example.autoutil.R.string.status_stopped)
    }

    private fun promptNewProfileName() {
        val input = EditText(this)
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("नया Profile नाम")
            .setView(input)
            .setPositiveButton("बनाएं") { _, _ ->
                val name = input.text.toString().ifBlank { "Profile" }
                viewModel.createProfile(name)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openAccessibilitySettings() {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
