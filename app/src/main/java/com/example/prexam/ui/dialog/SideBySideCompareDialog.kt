package com.example.prexam.ui.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import coil.load
import com.example.prexam.R
import com.example.prexam.data.local.DrugEntity
import com.example.prexam.databinding.DialogSideBySideCompareBinding

object SideBySideCompareDialog {

    fun show(context: Context, drug1: DrugEntity, drug2: DrugEntity) {
        val dialog = Dialog(context, android.R.style.Theme_Material_Light_NoActionBar_Fullscreen)
        val binding = DialogSideBySideCompareBinding.inflate(LayoutInflater.from(context))
        dialog.setContentView(binding.root)

        // Drug 1
        binding.tvCompName1.text = drug1.name
        binding.tvCompFormula1.text = "Formula: ${drug1.formula}"
        binding.tvCompMw1.text = "MW: ${drug1.mw} Da"
        binding.tvCompLogP1.text = "LogP: ${drug1.logP}"
        binding.tvCompHbd1.text = "HBD: ${drug1.hBondDonors} | HBA: ${drug1.hBondAcceptors}"
        binding.tvCompTpsa1.text = "TPSA: ${drug1.tpsa} Å²"

        binding.ivCompImage1.load("https://pubchem.ncbi.nlm.nih.gov/rest/pug/compound/name/${drug1.name}/PNG") {
            crossfade(true)
            placeholder(android.R.drawable.ic_menu_report_image)
            error(android.R.drawable.ic_menu_report_image)
        }

        if (drug1.lipinskiPasses) {
            binding.tvCompLipinski1.text = "Lipinski: PASS"
            binding.tvCompLipinski1.setBackgroundResource(R.drawable.bg_badge_pass)
            binding.tvCompLipinski1.setTextColor(Color.parseColor("#2E7D32"))
        } else {
            binding.tvCompLipinski1.text = "Lipinski: FAIL"
            binding.tvCompLipinski1.setBackgroundResource(R.drawable.bg_badge_fail)
            binding.tvCompLipinski1.setTextColor(Color.parseColor("#C62828"))
        }

        if (drug1.veberPasses) {
            binding.tvCompVeber1.text = "Veber: PASS"
            binding.tvCompVeber1.setBackgroundResource(R.drawable.bg_badge_pass)
            binding.tvCompVeber1.setTextColor(Color.parseColor("#2E7D32"))
        } else {
            binding.tvCompVeber1.text = "Veber: FAIL"
            binding.tvCompVeber1.setBackgroundResource(R.drawable.bg_badge_fail)
            binding.tvCompVeber1.setTextColor(Color.parseColor("#C62828"))
        }

        binding.tvCompOral1.text = "Oral: ${drug1.oralBioavailability}"
        binding.tvCompOral1.setBackgroundResource(if (drug1.oralBioavailabilitySafe) R.drawable.bg_chip_safe else R.drawable.bg_chip_risk)

        binding.tvCompBbb1.text = "BBB: ${drug1.bbbPenetration}"
        binding.tvCompBbb1.setBackgroundResource(if (drug1.bbbPenetrationSafe) R.drawable.bg_chip_safe else R.drawable.bg_chip_risk)

        binding.tvCompHerg1.text = "hERG: ${drug1.hergInhibition}"
        binding.tvCompHerg1.setBackgroundResource(if (drug1.hergInhibitionSafe) R.drawable.bg_chip_safe else R.drawable.bg_chip_risk)

        // Drug 2
        binding.tvCompName2.text = drug2.name
        binding.tvCompFormula2.text = "Formula: ${drug2.formula}"
        binding.tvCompMw2.text = "MW: ${drug2.mw} Da"
        binding.tvCompLogP2.text = "LogP: ${drug2.logP}"
        binding.tvCompHbd2.text = "HBD: ${drug2.hBondDonors} | HBA: ${drug2.hBondAcceptors}"
        binding.tvCompTpsa2.text = "TPSA: ${drug2.tpsa} Å²"

        binding.ivCompImage2.load("https://pubchem.ncbi.nlm.nih.gov/rest/pug/compound/name/${drug2.name}/PNG") {
            crossfade(true)
            placeholder(android.R.drawable.ic_menu_report_image)
            error(android.R.drawable.ic_menu_report_image)
        }

        if (drug2.lipinskiPasses) {
            binding.tvCompLipinski2.text = "Lipinski: PASS"
            binding.tvCompLipinski2.setBackgroundResource(R.drawable.bg_badge_pass)
            binding.tvCompLipinski2.setTextColor(Color.parseColor("#2E7D32"))
        } else {
            binding.tvCompLipinski2.text = "Lipinski: FAIL"
            binding.tvCompLipinski2.setBackgroundResource(R.drawable.bg_badge_fail)
            binding.tvCompLipinski2.setTextColor(Color.parseColor("#C62828"))
        }

        if (drug2.veberPasses) {
            binding.tvCompVeber2.text = "Veber: PASS"
            binding.tvCompVeber2.setBackgroundResource(R.drawable.bg_badge_pass)
            binding.tvCompVeber2.setTextColor(Color.parseColor("#2E7D32"))
        } else {
            binding.tvCompVeber2.text = "Veber: FAIL"
            binding.tvCompVeber2.setBackgroundResource(R.drawable.bg_badge_fail)
            binding.tvCompVeber2.setTextColor(Color.parseColor("#C62828"))
        }

        binding.tvCompOral2.text = "Oral: ${drug2.oralBioavailability}"
        binding.tvCompOral2.setBackgroundResource(if (drug2.oralBioavailabilitySafe) R.drawable.bg_chip_safe else R.drawable.bg_chip_risk)

        binding.tvCompBbb2.text = "BBB: ${drug2.bbbPenetration}"
        binding.tvCompBbb2.setBackgroundResource(if (drug2.bbbPenetrationSafe) R.drawable.bg_chip_safe else R.drawable.bg_chip_risk)

        binding.tvCompHerg2.text = "hERG: ${drug2.hergInhibition}"
        binding.tvCompHerg2.setBackgroundResource(if (drug2.hergInhibitionSafe) R.drawable.bg_chip_safe else R.drawable.bg_chip_risk)

        binding.btnCloseComparison.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}
