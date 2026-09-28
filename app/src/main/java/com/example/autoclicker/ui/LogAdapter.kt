package com.example.autoutil.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.autoutil.databinding.ItemLogBinding
import com.example.autoutil.feedback.LogEntry
import com.example.autoutil.feedback.LogRepository

class LogAdapter(private val logRepository: LogRepository) :
    RecyclerView.Adapter<LogAdapter.LogViewHolder>() {

    private val items = mutableListOf<LogEntry>()

    fun submitList(entries: List<LogEntry>) {
        items.clear()
        items.addAll(entries)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val binding = ItemLogBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LogViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        holder.bind(items[position], logRepository)
    }

    override fun getItemCount(): Int = items.size

    class LogViewHolder(private val binding: ItemLogBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(entry: LogEntry, logRepository: LogRepository) {
            binding.txtLogLine.text = logRepository.formatted(entry)
        }
    }
}
