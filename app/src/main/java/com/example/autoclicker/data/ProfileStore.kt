package com.example.autoutil.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONException
import java.io.File
import java.util.UUID

/**
 * Simple file-based profile persistence under the app's private files directory
 * (/data/data/<pkg>/files/profiles.json). No credentials are ever written here — see
 * ProfileJson and Profile for the fields that are actually persisted.
 */
class ProfileStore(context: Context) {

    private val file: File = File(context.filesDir, "profiles.json")

    @Synchronized
    fun loadAll(): List<Profile> {
        if (!file.exists()) return emptyList()
        return try {
            val text = file.readText()
            if (text.isBlank()) return emptyList()
            val array = JSONArray(text)
            (0 until array.length()).map { i -> ProfileJson.fromJson(array.getJSONObject(i)) }
        } catch (e: JSONException) {
            // Corrupt file: fail safe to an empty list rather than crash the app.
            emptyList()
        }
    }

    @Synchronized
    fun saveAll(profiles: List<Profile>) {
        val array = JSONArray()
        profiles.forEach { array.put(ProfileJson.toJson(it)) }
        file.writeText(array.toString())
    }

    fun upsert(profile: Profile) {
        val current = loadAll().toMutableList()
        val index = current.indexOfFirst { it.id == profile.id }
        if (index >= 0) current[index] = profile else current.add(profile)
        saveAll(current)
    }

    fun delete(profileId: String) {
        val current = loadAll().filterNot { it.id == profileId }
        saveAll(current)
    }

    companion object {
        fun newId(): String = UUID.randomUUID().toString()
    }
}
