package com.example.autoutil.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.autoutil.data.Step
import com.example.autoutil.databinding.ItemStepBinding

class StepAdapter(
    private val onEdit: (Step) -> Unit,
    private val onDelete: (Step) -> Unit
) : RecyclerView.Adapter<StepAdapter.StepViewHolder>() {

    private val items = mutableListOf<Step>()

    fun submitList(steps: List<Step>) {
        items.clear()
        items.addAll(steps)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StepViewHolder {
        val binding = ItemStepBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return StepViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StepViewHolder, position: Int) {
        val step = items[position]
        holder.bind(position + 1, step, onEdit, onDelete)
    }

    override fun getItemCount(): Int = items.size

    class StepViewHolder(private val binding: ItemStepBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(index: Int, step: Step, onEdit: (Step) -> Unit, onDelete: (Step) -> Unit) {
            binding.txtStepTitle.text = "$index. ${step.action} — ${step.targetType}"
            binding.txtStepDetail.text =
                "target='${step.targetValue}'  delay=${step.delayMs}ms  timeout=${step.timeoutMs}ms  retry=${step.maxRetries}"
            binding.btnEditStep.setOnClickListener { onEdit(step) }
            binding.btnDeleteStep.setOnClickListener { onDelete(step) }
        }
    }
}
