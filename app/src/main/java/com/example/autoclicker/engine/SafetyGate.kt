package com.example.autoutil.engine

import android.view.accessibility.AccessibilityNodeInfo

/**
 * Refuses to let the engine automate sensitive fields, regardless of how a step was
 * configured. This is a hard boundary, not a user preference: OTP, password, PIN and
 * CAPTCHA-looking targets always stop the workflow and hand control back to the user.
 */
object SafetyGate {

    private val SENSITIVE_KEYWORDS = listOf(
        "otp", "one time password", "one-time password",
        "password", "पासवर्ड",
        "pin", "पिन",
        "cvv", "cvc",
        "captcha", "कैप्चा",
        "card number", "card no",
        "security code"
    )

    /**
     * @return a human-readable reason if [node] looks sensitive, or null if it's safe to
     * proceed with the configured action.
     */
    fun checkNode(node: AccessibilityNodeInfo): String? {
        // 1. Android's own password flag on EditText fields.
        if (node.isPassword) {
            return "Field Android द्वारा password के रूप में चिह्नित है"
        }

        // 2. Text / hint / contentDescription / viewId keyword screening. Android does not
        // expose a raw InputType flag on AccessibilityNodeInfo across all API levels, so
        // node.isPassword (above) plus this keyword screen are the primary signals used here.
        val haystack = buildString {
            append(node.text ?: "")
            append(' ')
            append(node.hintText ?: "")
            append(' ')
            append(node.contentDescription ?: "")
            append(' ')
            append(node.viewIdResourceName ?: "")
        }.lowercase()

        for (keyword in SENSITIVE_KEYWORDS) {
            if (haystack.contains(keyword)) {
                return "Field/label में sensitive keyword मिला: '$keyword'"
            }
        }

        return null
    }

    /** Screens a dialog window's root text for CAPTCHA / sensitive-authorization content. */
    fun checkWindowText(windowText: String): String? {
        val lower = windowText.lowercase()
        for (keyword in SENSITIVE_KEYWORDS) {
            if (lower.contains(keyword)) {
                return "Screen पर sensitive keyword मिला: '$keyword'"
            }
        }
        return null
    }
}
