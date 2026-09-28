package com.example.autoutil.engine

import com.example.autoutil.data.ActionType
import com.example.autoutil.data.Profile
import com.example.autoutil.feedback.CompletionNotifier
import com.example.autoutil.feedback.LogRepository
import com.example.autoutil.feedback.SnapshotManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** Result of attempting a single step, including reasons that must stop the whole workflow. */
private sealed class StepRunResult {
    object Success : StepRunResult()
    object Failed : StepRunResult()
    data class SafetyStop(val reason: String) : StepRunResult()
    data class UnknownInterruptionStop(val hint: String) : StepRunResult()
    object EngineStopped : StepRunResult()
}

/**
 * Core sequential orchestrator: Target -> Action -> Verify -> Next Step, with per-step delay,
 * timeout and bounded retry. A failed verification never lets the workflow advance; an unknown
 * interruption or a safety-gated field always stops the entire workflow rather than just the
 * current step, per the app's safety boundary.
 */
class WorkflowEngine(
    private val rootProvider: AccessibilityRootProvider,
    private val logRepository: LogRepository,
    private val snapshotManager: SnapshotManager,
    private val completionNotifier: CompletionNotifier,
    private val onStateChanged: (status: WorkflowStatus, stepIndex: Int, stepCount: Int) -> Unit
) {
    private val nodeFinder = NodeFinder()
    private val interruptionGuard = InterruptionGuard(nodeFinder)
    private val inputHandler = InputHandler()
    private val actionExecutor = ActionExecutor(inputHandler)
    private val verifier = Verifier(nodeFinder)

    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    @Volatile
    private var stopRequested = false

    fun isRunning(): Boolean = job?.isActive == true

    fun start(profile: Profile, userText: String) {
        if (isRunning()) return
        stopRequested = false

        job = scope.launch {
            runWorkflow(profile, userText)
        }
    }

    /** Emergency stop: cancels the workflow immediately, whatever step it is on. */
    fun stop() {
        stopRequested = true
        job?.cancel()
        onStateChanged(WorkflowStatus.STOPPED, -1, 0)
        logRepository.log("Workflow STOP द्वारा रोका गया")
        completionNotifier.notifyCompletion(WorkflowStatus.STOPPED, vibrate = true, sound = false)
    }

    private suspend fun runWorkflow(profile: Profile, userText: String) {
        val variableResolver = VariableResolver(profile.counterStart, profile.counterMax)
        logRepository.log("Workflow शुरू: ${profile.name} (${profile.steps.size} steps)")
        onStateChanged(WorkflowStatus.RUNNING, 0, profile.steps.size)

        for ((index, step) in profile.steps.withIndex()) {
            if (stopRequested || !scope.isActive) return

            onStateChanged(WorkflowStatus.RUNNING, index, profile.steps.size)
            logRepository.log("Step ${index + 1}/${profile.steps.size} शुरू: ${step.action} -> ${step.targetType}=${step.targetValue}")

            if (step.delayMs > 0) delay(step.delayMs)

            val result = withTimeoutOrNull(step.timeoutMs) {
                runStepWithRetry(step, profile, variableResolver, userText)
            } ?: StepRunResult.Failed.also {
                logRepository.log("Step ${index + 1} TIMEOUT (${step.timeoutMs}ms)")
            }

            when (result) {
                is StepRunResult.Success -> {
                    logRepository.log("Step ${index + 1} verified ✔")
                }
                is StepRunResult.Failed -> {
                    logRepository.log("Step ${index + 1} विफल — workflow रोका गया")
                    finish(WorkflowStatus.FAILED, profile)
                    return
                }
                is StepRunResult.SafetyStop -> {
                    logRepository.log("Safety boundary: ${result.reason} — workflow रोका गया, user control में है")
                    finish(WorkflowStatus.STOPPED, profile)
                    return
                }
                is StepRunResult.UnknownInterruptionStop -> {
                    logRepository.log("अज्ञात popup/interruption मिला — workflow रोका गया: ${result.hint}")
                    finish(WorkflowStatus.STOPPED, profile)
                    return
                }
                is StepRunResult.EngineStopped -> {
                    return
                }
            }
        }

        finish(WorkflowStatus.COMPLETED, profile)
    }

    private suspend fun runStepWithRetry(
        step: com.example.autoutil.data.Step,
        profile: Profile,
        variableResolver: VariableResolver,
        userText: String
    ): StepRunResult {
        var attempt = 0

        while (attempt <= step.maxRetries) {
            if (stopRequested) return StepRunResult.EngineStopped

            val root = rootProvider.getRootNode()
            if (root == null) {
                attempt++
                delay(200)
                continue
            }

            // 1. Interruption check, bounded to a few handled popups per attempt so a
            // misconfigured allow-list can't loop forever.
            var interruptionHandles = 0
            var currentRoot = root
            while (interruptionHandles < 3) {
                val check = interruptionGuard.check(currentRoot, profile.allowedInterruptions)
                when (check) {
                    is InterruptionCheck.Handled -> {
                        logRepository.log("Known popup handled: ${check.rule.dialogTextContains}")
                        interruptionGuard.performHandledAction(currentRoot, check.rule)
                        interruptionHandles++
                        delay(300)
                        currentRoot = rootProvider.getRootNode() ?: return StepRunResult.Failed
                    }
                    is InterruptionCheck.UnknownBlocking -> {
                        return StepRunResult.UnknownInterruptionStop(check.windowText)
                    }
                    InterruptionCheck.None -> break
                }
            }

            // 2. Locate target (targeted search first, hierarchy fallback inside NodeFinder).
            val node = nodeFinder.find(currentRoot, step.targetType, step.targetValue)
            if (node == null) {
                logRepository.log("Step target नहीं मिला (attempt ${attempt + 1}/${step.maxRetries + 1})")
                attempt++
                delay(250)
                continue
            }

            // 3. Safety boundary — never automate sensitive fields.
            val safetyReason = SafetyGate.checkNode(node)
            if (safetyReason != null) {
                return StepRunResult.SafetyStop(safetyReason)
            }

            // 4. Resolve variables (only meaningful for INPUT) and execute.
            val resolvedInput = if (step.action == ActionType.INPUT) {
                variableResolver.resolve(step.inputTemplate, userText)
            } else ""

            val performed = actionExecutor.execute(node, step.action, resolvedInput)
            if (!performed) {
                logRepository.log("Action execute नहीं हुआ (attempt ${attempt + 1}/${step.maxRetries + 1})")
                attempt++
                delay(250)
                continue
            }
            logRepository.log("Action executed: ${step.action}")

            // 5. Verify.
            delay(150)
            val freshRoot = rootProvider.getRootNode() ?: currentRoot
            val verified = verifier.verify(freshRoot, step, node)
            if (verified) {
                return StepRunResult.Success
            }

            logRepository.log("Verification विफल (attempt ${attempt + 1}/${step.maxRetries + 1})")
            if (profile.snapshotEnabled && !step.sensitive) {
                val bytes = rootProvider.captureSnapshot()
                if (bytes != null) {
                    val path = snapshotManager.save(bytes, step.id)
                    if (path != null) logRepository.log("Diagnostic snapshot saved")
                }
            }
            attempt++
        }

        return StepRunResult.Failed
    }

    private fun finish(status: WorkflowStatus, profile: Profile) {
        onStateChanged(status, -1, profile.steps.size)
        completionNotifier.notifyCompletion(
            status = status,
            vibrate = profile.completionVibrate,
            sound = profile.completionSound
        )
    }
}
