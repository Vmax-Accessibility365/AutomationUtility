package com.example.autoutil.engine

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Resolves {{DATE}}, {{TIME}}, {{COUNTER}}, {{USER_TEXT}} inside a step's input template.
 * COUNTER is bounded by [counterMax] (wraps back to [counterStart]) so a misconfigured
 * profile cannot silently produce unbounded/unexpected values.
 */
class VariableResolver(
    private val counterStart: Int,
    private val counterMax: Int
) {
    private var counter: Int = counterStart

    fun resolve(template: String, userText: String): String {
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())

        var result = template
        result = result.replace("{{DATE}}", dateStr)
        result = result.replace("{{TIME}}", timeStr)
        result = result.replace("{{USER_TEXT}}", userText)

        if (result.contains("{{COUNTER}}")) {
            result = result.replace("{{COUNTER}}", counter.toString())
            counter = if (counter >= counterMax) counterStart else counter + 1
        }
        return result
    }

    fun currentCounter(): Int = counter

    fun resetCounter() {
        counter = counterStart
    }
}
