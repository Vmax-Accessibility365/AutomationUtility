package com.example.autoutil.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.autoutil.data.Profile
import com.example.autoutil.data.ProfileStore
import com.example.autoutil.data.Step
import com.example.autoutil.engine.AutomationController

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val profileStore = ProfileStore(application)

    private val _profiles = MutableLiveData<List<Profile>>(emptyList())
    val profiles: LiveData<List<Profile>> = _profiles

    private val _activeProfile = MutableLiveData<Profile?>(null)
    val activeProfile: LiveData<Profile?> = _activeProfile

    val logEntries = AutomationController.logRepository.entries
    val workflowStatus = AutomationController.statusLiveData
    val currentStep = AutomationController.currentStepLiveData

    init {
        AutomationController.init(application)
        reload()
    }

    fun reload() {
        val loaded = profileStore.loadAll()
        _profiles.value = loaded
        if (_activeProfile.value == null) {
            _activeProfile.value = loaded.firstOrNull()
        }
    }

    fun selectProfile(profile: Profile) {
        _activeProfile.value = profile
    }

    fun createProfile(name: String) {
        val profile = Profile(id = ProfileStore.newId(), name = name)
        profileStore.upsert(profile)
        reload()
        _activeProfile.value = profile
    }

    fun saveActiveProfile() {
        val profile = _activeProfile.value ?: return
        profileStore.upsert(profile)
        reload()
    }

    fun addStep(step: Step) {
        val profile = _activeProfile.value ?: return
        val updated = profile.copy(steps = profile.steps + step)
        _activeProfile.value = updated
    }

    fun updateStep(step: Step) {
        val profile = _activeProfile.value ?: return
        val updated = profile.copy(
            steps = profile.steps.map { if (it.id == step.id) step else it }
        )
        _activeProfile.value = updated
    }

    fun deleteStep(stepId: String) {
        val profile = _activeProfile.value ?: return
        val updated = profile.copy(steps = profile.steps.filterNot { it.id == stepId })
        _activeProfile.value = updated
    }

    fun startWorkflow(userText: String) {
        val profile = _activeProfile.value ?: return
        AutomationController.start(profile, userText)
    }

    fun stopWorkflow() {
        AutomationController.stop()
    }

    fun isAccessibilityReady(): Boolean = AutomationController.isAccessibilityReady()
}
