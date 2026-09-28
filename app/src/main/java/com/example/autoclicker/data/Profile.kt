package com.example.autoutil.data

/**
 * A saved, reusable workflow. Profiles never hold credentials: there is deliberately no field
 * here for passwords, OTP, or PIN values. See SafetyGate for the runtime guard that blocks
 * automated interaction with such fields even if a step is misconfigured to target one.
 */
data class Profile(
    val id: String,
    val name: String,
    val steps: List<Step> = emptyList(),
    val allowedInterruptions: List<InterruptionRule> = emptyList(),
    val snapshotEnabled: Boolean = false,
    val completionVibrate: Boolean = true,
    val completionSound: Boolean = false,
    val counterStart: Int = 1,
    val counterMax: Int = 9999
)
