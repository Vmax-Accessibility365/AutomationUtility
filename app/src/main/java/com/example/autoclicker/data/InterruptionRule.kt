package com.example.autoutil.data

/**
 * A single explicitly-allowed popup/dialog handling rule. InterruptionGuard only ever acts on
 * rules the user has configured for the active profile — an unrecognised dialog is never
 * touched; the workflow pauses and the user is notified instead.
 *
 * @param dialogTextContains a substring expected somewhere in the dialog window (title or body)
 *        used to decide whether this rule applies to the currently foregrounded dialog
 * @param buttonTargetType how [buttonTargetValue] is matched (usually TEXT, e.g. "OK", "Close")
 * @param buttonTargetValue the value of the button to press inside the matched dialog
 */
data class InterruptionRule(
    val dialogTextContains: String,
    val buttonTargetType: TargetType,
    val buttonTargetValue: String
)
