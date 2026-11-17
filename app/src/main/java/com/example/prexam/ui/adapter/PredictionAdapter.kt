package com.example.prexam.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import biokt.Prediction
import com.example.prexam.databinding.ItemPredictionBinding

class PredictionAdapter(
    private var items: List<Prediction> = emptyList()
) : RecyclerView.Adapter<PredictionAdapter.PredictionViewHolder>() {

    fun updateItems(newItems: List<Prediction>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PredictionViewHolder {
        val binding = ItemPredictionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PredictionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PredictionViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class PredictionViewHolder(private val binding: ItemPredictionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Prediction) {
            binding.tvPredictionLabel.text = item.label
            binding.tvConfidenceText.text = "${item.confidence}%"
            binding.progressConfidence.progress = item.confidence.toInt().coerceIn(0, 100)
        }
    }
}
