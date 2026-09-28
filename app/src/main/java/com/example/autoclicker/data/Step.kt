package com.example.autoutil.data

/**
 * A single sequential automation step.
 *
 * @param id stable identifier (used for logs and editing, not for UI ordering)
 * @param targetType how [targetValue] should be matched against on-screen nodes
 * @param targetValue the viewId / text / contentDescription to search for
 * @param action what to do once a valid, visible node is found
 * @param inputTemplate raw template text for ACTION.INPUT, may contain {{DATE}} {{TIME}}
 *        {{COUNTER}} {{USER_TEXT}}. Never used to carry OTP/password/PIN values.
 * @param verifyValue optional text/viewId that must be present after the action for the step
 *        to be considered verified. If blank, verification only re-checks that the target
 *        node still resolves and is in a stable (non-stale) state.
 * @param delayMs delay applied before this step starts (lets the previous screen settle)
 * @param timeoutMs maximum time to wait for target-find + verification before failing the step
 * @param maxRetries bounded number of retries after a failed verification (0 = no retry)
 * @param sensitive when true, SnapshotManager will never capture a diagnostic image for this
 *        step even if snapshots are enabled at the profile level
 */
data class Step(
    val id: String,
    val targetType: TargetType,
    val targetValue: String,
    val action: ActionType,
    val inputTemplate: String = "",
    val verifyValue: String = "",
    val delayMs: Long = 500L,
    val timeoutMs: Long = 8000L,
    val maxRetries: Int = 2,
    val sensitive: Boolean = false
)
