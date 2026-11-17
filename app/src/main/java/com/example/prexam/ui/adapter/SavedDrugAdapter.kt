package com.example.prexam.ui.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.prexam.R
import com.example.prexam.data.local.DrugEntity
import com.example.prexam.databinding.ItemSavedDrugBinding

class SavedDrugAdapter(
    private var items: List<DrugEntity> = emptyList(),
    private var selectedIds: Set<Long> = emptySet(),
    private val onToggleSelect: (Long) -> Unit,
    private val onDelete: (DrugEntity) -> Unit,
    private val onItemClick: (DrugEntity) -> Unit
) : RecyclerView.Adapter<SavedDrugAdapter.SavedDrugViewHolder>() {

    fun updateData(newItems: List<DrugEntity>, newSelectedIds: Set<Long>) {
        items = newItems
        selectedIds = newSelectedIds
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SavedDrugViewHolder {
        val binding = ItemSavedDrugBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SavedDrugViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SavedDrugViewHolder, position: Int) {
        holder.bind(items[position], selectedIds.contains(items[position].id))
    }

    override fun getItemCount(): Int = items.size

    inner class SavedDrugViewHolder(private val binding: ItemSavedDrugBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: DrugEntity, isSelected: Boolean) {
            binding.tvSavedDrugName.text = item.name
            binding.tvSavedFormula.text = "Formula: ${item.formula} | MW: ${item.mw} Da | LogP: ${item.logP}"

            if (item.lipinskiPasses) {
                binding.tvLipinskiBadge.text = "Lipinski: PASS"
                binding.tvLipinskiBadge.setBackgroundResource(R.drawable.bg_badge_pass)
                binding.tvLipinskiBadge.setTextColor(Color.parseColor("#2E7D32"))
            } else {
                binding.tvLipinskiBadge.text = "Lipinski: FAIL"
                binding.tvLipinskiBadge.setBackgroundResource(R.drawable.bg_badge_fail)
                binding.tvLipinskiBadge.setTextColor(Color.parseColor("#C62828"))
            }

            if (item.veberPasses) {
                binding.tvVeberBadge.text = "Veber: PASS"
                binding.tvVeberBadge.setBackgroundResource(R.drawable.bg_badge_pass)
                binding.tvVeberBadge.setTextColor(Color.parseColor("#2E7D32"))
            } else {
                binding.tvVeberBadge.text = "Veber: FAIL"
                binding.tvVeberBadge.setBackgroundResource(R.drawable.bg_badge_fail)
                binding.tvVeberBadge.setTextColor(Color.parseColor("#C62828"))
            }

            binding.cbCompareSelect.setOnCheckedChangeListener(null)
            binding.cbCompareSelect.isChecked = isSelected
            binding.cbCompareSelect.setOnCheckedChangeListener { _, _ ->
                onToggleSelect(item.id)
            }

            binding.btnDeleteSaved.setOnClickListener {
                onDelete(item)
            }

            binding.root.setOnClickListener {
                onItemClick(item)
            }
        }
    }
}
