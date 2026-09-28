package com.example.autoutil.data

/**
 * Supported actions on a located, visible UI node.
 * INPUT is restricted to plain editable fields; SafetyGate blocks password/OTP/PIN fields
 * regardless of what the step author configured.
 */
enum class ActionType {
    CLICK,
    INPUT,
    SCROLL_FORWARD,
    SCROLL_BACKWARD,
    WAIT
}
