package com.example.autoutil.engine

/** High-level lifecycle state of the workflow engine, observed by the UI. */
enum class WorkflowStatus {
    IDLE,
    RUNNING,
    COMPLETED,
    FAILED,
    STOPPED
}

/** Outcome of a single step attempt, used for logging. */
sealed class StepOutcome {
    object Started : StepOutcome()
    object ActionExecuted : StepOutcome()
    object VerificationPassed : StepOutcome()
    data class VerificationFailed(val attempt: Int, val maxRetries: Int) : StepOutcome()
    data class InterruptionHandled(val dialogHint: String) : StepOutcome()
    data class InterruptionBlocked(val dialogHint: String) : StepOutcome()
    data class SafetyBlocked(val reason: String) : StepOutcome()
    object TimedOut : StepOutcome()
    object TargetNotFound : StepOutcome()
}
