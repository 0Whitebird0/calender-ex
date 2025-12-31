package com.example.calender_ex

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.example.calender_ex.databinding.ItemEventBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EventAdapter(
    private val onEdit: (EventUiModel) -> Unit,
    private val onDelete: (EventUiModel) -> Unit
) : ListAdapter<EventUiModel, EventAdapter.EventViewHolder>(EventDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val binding = ItemEventBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return EventViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class EventViewHolder(private val binding: ItemEventBinding) :
        androidx.recyclerview.widget.RecyclerView.ViewHolder(binding.root) {
        fun bind(event: EventUiModel) {
            binding.eventTitle.text = event.title
            val timeFormat = SimpleDateFormat("HH:mm", Locale.KOREA)
            binding.eventTime.text = "${timeFormat.format(Date(event.startTime))} - ${timeFormat.format(Date(event.endTime))}"
            binding.eventDescription.text = event.description

            binding.root.setOnClickListener { onEdit(event) }
            binding.root.setOnLongClickListener {
                onDelete(event)
                true
            }
        }
    }
}

class EventDiffCallback : DiffUtil.ItemCallback<EventUiModel>() {
    override fun areItemsTheSame(oldItem: EventUiModel, newItem: EventUiModel): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: EventUiModel, newItem: EventUiModel): Boolean {
        return oldItem == newItem
    }
}
