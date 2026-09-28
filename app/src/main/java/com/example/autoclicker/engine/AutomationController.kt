package com.example.autoutil.engine

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.autoutil.data.Profile
import com.example.autoutil.feedback.CompletionNotifier
import com.example.autoutil.feedback.LogRepository
import com.example.autoutil.feedback.SnapshotManager

/**
 * App-wide coordination point between the UI (MainActivity), the Accessibility Service (which
 * supplies the live AccessibilityRootProvider once the user has turned the service on) and the
 * Foreground Service (which shows the running-workflow notification with its own STOP action).
 *
 * There is exactly one WorkflowEngine instance alive at a time, created fresh for each run.
 */
object AutomationController {

    val logRepository = LogRepository()
    val statusLiveData = MutableLiveData(WorkflowStatus.IDLE)
    /** first = current step index (0-based, -1 when not on a step), second = total step count */
    val currentStepLiveData = MutableLiveData(-1 to 0)

    private var snapshotManager: SnapshotManager? = null
    private var completionNotifier: CompletionNotifier? = null
    private var engine: WorkflowEngine? = null

    @Volatile
    private var rootProvider: AccessibilityRootProvider? = null

    fun init(context: Context) {
        val appContext = context.applicationContext
        if (snapshotManager == null) snapshotManager = SnapshotManager(appContext)
        if (completionNotifier == null) completionNotifier = CompletionNotifier(appContext)
    }

    fun attachProvider(provider: AccessibilityRootProvider) {
        rootProvider = provider
        logRepository.log("Accessibility Service जुड़ी")
    }

    fun detachProvider() {
        rootProvider = null
        if (isRunning()) {
            stop()
        }
        logRepository.log("Accessibility Service अलग हुई")
    }

    fun isAccessibilityReady(): Boolean = rootProvider != null

    fun isRunning(): Boolean = engine?.isRunning() == true

    fun start(profile: Profile, userText: String) {
        val provider = rootProvider
        if (provider == null) {
            logRepository.log("शुरू नहीं हो सका: पहले Accessibility Service चालू करें")
            return
        }
        if (isRunning()) {
            logRepository.log("एक workflow पहले से चल रहा है")
            return
        }
        val snapshot = snapshotManager ?: return
        val notifier = completionNotifier ?: return

        val newEngine = WorkflowEngine(
            rootProvider = provider,
            logRepository = logRepository,
            snapshotManager = snapshot,
            completionNotifier = notifier
        ) { status, stepIndex, stepCount ->
            statusLiveData.postValue(status)
            currentStepLiveData.postValue(stepIndex to stepCount)
        }
        engine = newEngine
        newEngine.start(profile, userText)
    }

    /** Emergency stop — callable from the UI STOP button, the foreground notification action,
     *  or the optional volume-key handler in the Accessibility Service. */
    fun stop() {
        engine?.stop()
    }
}
