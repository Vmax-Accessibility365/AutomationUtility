package com.example.autoutil.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * Manual JSON (de)serialization for Profile using android.util / org.json (no extra
 * dependency needed). Kept deliberately explicit so it is obvious no credential field ever
 * gets serialized: only the fields declared below are written or read.
 */
object ProfileJson {

    fun toJson(profile: Profile): JSONObject = JSONObject().apply {
        put("id", profile.id)
        put("name", profile.name)
        put("snapshotEnabled", profile.snapshotEnabled)
        put("completionVibrate", profile.completionVibrate)
        put("completionSound", profile.completionSound)
        put("counterStart", profile.counterStart)
        put("counterMax", profile.counterMax)

        val stepsArray = JSONArray()
        profile.steps.forEach { stepsArray.put(stepToJson(it)) }
        put("steps", stepsArray)

        val rulesArray = JSONArray()
        profile.allowedInterruptions.forEach { rulesArray.put(ruleToJson(it)) }
        put("allowedInterruptions", rulesArray)
    }

    fun fromJson(obj: JSONObject): Profile {
        val steps = mutableListOf<Step>()
        val stepsArray = obj.optJSONArray("steps") ?: JSONArray()
        for (i in 0 until stepsArray.length()) {
            steps.add(stepFromJson(stepsArray.getJSONObject(i)))
        }

        val rules = mutableListOf<InterruptionRule>()
        val rulesArray = obj.optJSONArray("allowedInterruptions") ?: JSONArray()
        for (i in 0 until rulesArray.length()) {
            rules.add(ruleFromJson(rulesArray.getJSONObject(i)))
        }

        return Profile(
            id = obj.getString("id"),
            name = obj.getString("name"),
            steps = steps,
            allowedInterruptions = rules,
            snapshotEnabled = obj.optBoolean("snapshotEnabled", false),
            completionVibrate = obj.optBoolean("completionVibrate", true),
            completionSound = obj.optBoolean("completionSound", false),
            counterStart = obj.optInt("counterStart", 1),
            counterMax = obj.optInt("counterMax", 9999)
        )
    }

    private fun stepToJson(step: Step): JSONObject = JSONObject().apply {
        put("id", step.id)
        put("targetType", step.targetType.name)
        put("targetValue", step.targetValue)
        put("action", step.action.name)
        put("inputTemplate", step.inputTemplate)
        put("verifyValue", step.verifyValue)
        put("delayMs", step.delayMs)
        put("timeoutMs", step.timeoutMs)
        put("maxRetries", step.maxRetries)
        put("sensitive", step.sensitive)
    }

    private fun stepFromJson(obj: JSONObject): Step = Step(
        id = obj.getString("id"),
        targetType = TargetType.valueOf(obj.getString("targetType")),
        targetValue = obj.getString("targetValue"),
        action = ActionType.valueOf(obj.getString("action")),
        inputTemplate = obj.optString("inputTemplate", ""),
        verifyValue = obj.optString("verifyValue", ""),
        delayMs = obj.optLong("delayMs", 500L),
        timeoutMs = obj.optLong("timeoutMs", 8000L),
        maxRetries = obj.optInt("maxRetries", 2),
        sensitive = obj.optBoolean("sensitive", false)
    )

    private fun ruleToJson(rule: InterruptionRule): JSONObject = JSONObject().apply {
        put("dialogTextContains", rule.dialogTextContains)
        put("buttonTargetType", rule.buttonTargetType.name)
        put("buttonTargetValue", rule.buttonTargetValue)
    }

    private fun ruleFromJson(obj: JSONObject): InterruptionRule = InterruptionRule(
        dialogTextContains = obj.getString("dialogTextContains"),
        buttonTargetType = TargetType.valueOf(obj.getString("buttonTargetType")),
        buttonTargetValue = obj.getString("buttonTargetValue")
    )
}
