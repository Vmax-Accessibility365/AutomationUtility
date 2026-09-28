package com.example.autoutil.ui

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import com.example.autoutil.data.ActionType
import com.example.autoutil.data.Step
import com.example.autoutil.data.TargetType
import com.example.autoutil.databinding.DialogEditStepBinding
import com.example.autoutil.data.ProfileStore

/** Builds/edits a single Step via a simple form dialog. */
object StepEditDialog {

    fun show(context: Context, existing: Step?, onSave: (Step) -> Unit) {
        val binding = DialogEditStepBinding.inflate(LayoutInflater.from(context))

        val targetTypeAdapter = ArrayAdapter(
            context, android.R.layout.simple_spinner_dropdown_item, TargetType.values()
        )
        binding.spinnerTargetType.adapter = targetTypeAdapter

        val actionAdapter = ArrayAdapter(
            context, android.R.layout.simple_spinner_dropdown_item, ActionType.values()
        )
        binding.spinnerAction.adapter = actionAdapter

        if (existing != null) {
            binding.spinnerTargetType.setSelection(targetTypeAdapter.getPosition(existing.targetType))
            binding.spinnerAction.setSelection(actionAdapter.getPosition(existing.action))
            binding.editTargetValue.setText(existing.targetValue)
            binding.editInputTemplate.setText(existing.inputTemplate)
            binding.editVerifyValue.setText(existing.verifyValue)
            binding.editDelayMs.setText(existing.delayMs.toString())
            binding.editTimeoutMs.setText(existing.timeoutMs.toString())
            binding.editRetryCount.setText(existing.maxRetries.toString())
            binding.checkSensitive.isChecked = existing.sensitive
        } else {
            binding.editDelayMs.setText("500")
            binding.editTimeoutMs.setText("8000")
            binding.editRetryCount.setText("2")
        }

        AlertDialog.Builder(context)
            .setTitle(if (existing == null) "नया Step" else "Step Edit करें")
            .setView(binding.root)
            .setPositiveButton("Save") { _, _ ->
                val step = Step(
                    id = existing?.id ?: ProfileStore.newId(),
                    targetType = targetTypeAdapter.getItem(binding.spinnerTargetType.selectedItemPosition)
                        ?: TargetType.TEXT,
                    targetValue = binding.editTargetValue.text.toString(),
                    action = actionAdapter.getItem(binding.spinnerAction.selectedItemPosition)
                        ?: ActionType.CLICK,
                    inputTemplate = binding.editInputTemplate.text.toString(),
                    verifyValue = binding.editVerifyValue.text.toString(),
                    delayMs = binding.editDelayMs.text.toString().toLongOrNull() ?: 500L,
                    timeoutMs = binding.editTimeoutMs.text.toString().toLongOrNull() ?: 8000L,
                    maxRetries = binding.editRetryCount.text.toString().toIntOrNull() ?: 2,
                    sensitive = binding.checkSensitive.isChecked
                )
                onSave(step)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
