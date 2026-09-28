package com.example.autoutil.feedback

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class LogEntry(val timestamp: Long, val message: String)

/**
 * In-memory execution log shown live in the UI. Deliberately sanitizes anything that looks
 * like it could be sensitive before it is ever appended — see [sanitize]. Log entries are not
 * persisted to disk; they exist only for the current session.
 */
class LogRepository {

    private val _entries = MutableLiveData<List<LogEntry>>(emptyList())
    val entries: LiveData<List<LogEntry>> = _entries

    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    fun log(message: String) {
        val safeMessage = sanitize(message)
        val now = System.currentTimeMillis()
        val updated = (_entries.value ?: emptyList()) + LogEntry(now, safeMessage)
        _entries.postValue(updated)
    }

    fun formatted(entry: LogEntry): String {
        return "[${timeFormat.format(Date(entry.timestamp))}] ${entry.message}"
    }

    fun clear() {
        _entries.postValue(emptyList())
    }

    /**
     * Redacts common sensitive-looking substrings (long digit runs that could be OTP/PIN/card
     * numbers) as a defensive second layer on top of SafetyGate, which should already prevent
     * the engine from acting on such fields in the first place.
     */
    private fun sanitize(message: String): String {
        val digitRun = Regex("\\d{4,}")
        return digitRun.replace(message) { "****" }
    }
}
