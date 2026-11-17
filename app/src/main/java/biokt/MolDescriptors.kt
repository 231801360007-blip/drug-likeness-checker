package biokt

import kotlin.math.round

data class AdmetResult(
    val label: String,
    val isSafe: Boolean, // true = safe (green), false = risk (red)
    val details: String = ""
)

data class Descriptors(
    val mw: Double,
    val logP: Double,
    val hBondDonors: Int,
    val hBondAcceptors: Int,
    val tpsa: Double,
    val rotatableBonds: Int,
    val lipinskiPasses: Boolean,
    val veberPasses: Boolean,
    val oralBioavailability: AdmetResult,
    val bbbPenetration: AdmetResult,
    val hergInhibition: AdmetResult
)

object MolDescriptors {

    fun calculate(molecule: Molecule): Descriptors {
        val smiles = molecule.smiles.uppercase()

        // Count atoms
        val cCount = smiles.count { it == 'C' }
        val nCount = smiles.count { it == 'N' }
        val oCount = smiles.count { it == 'O' }
        val fCount = smiles.count { it == 'F' }
        val clCount = smiles.contains("CL").let { if (it) smiles.windowed(2).count { w -> w == "CL" } else 0 }
        val brCount = smiles.contains("BR").let { if (it) smiles.windowed(2).count { w -> w == "BR" } else 0 }
        val sCount = smiles.count { it == 'S' }
        val pCount = smiles.count { it == 'P' }

        // Molecular Weight calculation
        var rawMw = (cCount * 12.011) + (nCount * 14.007) + (oCount * 15.999) +
                (fCount * 18.998) + (clCount * 35.453) + (brCount * 79.904) +
                (sCount * 32.06) + (pCount * 30.974)

        // Estimated implicit hydrogens if SMILES has standard valences
        val estimatedH = (cCount * 2 + nCount * 1 + oCount * 1 + 2).coerceAtLeast(4)
        rawMw += estimatedH * 1.008

        // If MW is tiny (fallback for short SMILES), scale to realistic drug MW range
        val mw = round(rawMw * 100) / 100.0

        // H-Bond Donors (OH, NH)
        val hbd = (smiles.windowed(2).count { it == "OH" || it == "NH" } +
                if (smiles.contains("NH2") || smiles.contains("NH3")) 1 else 0).coerceAtLeast(
            if (oCount > 0 || nCount > 0) (oCount + nCount) / 3 else 0
        )

        // H-Bond Acceptors (N, O)
        val hba = (nCount + oCount).coerceAtLeast(1)

        // Estimated LogP
        val estimatedLogP = (cCount * 0.25) + (clCount * 0.5) + (brCount * 0.7) + (fCount * 0.1) -
                (oCount * 0.3) - (nCount * 0.2)
        val logP = round(estimatedLogP * 100) / 100.0

        // TPSA calculation (approx 14.0 per O, 12.0 per N/OH)
        val tpsa = round((oCount * 17.07 + nCount * 12.89 + hbd * 9.23) * 100) / 100.0

        // Rotatable bonds count (single bonds not in rings)
        val rotatableBonds = (smiles.count { it == '-' } + (cCount / 4)).coerceAtMost(15)

        // Lipinski Rule of 5: MW <= 500, LogP <= 5, HBD <= 5, HBA <= 10
        val lipinskiPasses = mw <= 500.0 && logP <= 5.0 && hbd <= 5 && hba <= 10

        // Veber Rule: TPSA <= 140, Rotatable Bonds <= 10
        val veberPasses = tpsa <= 140.0 && rotatableBonds <= 10

        // ADMET calculations
        // Oral Bioavailability: Good if Lipinski & Veber pass and LogP in [0, 5]
        val oralBioavailability = if (lipinskiPasses && veberPasses) {
            AdmetResult("High Bioavailability (Safe)", true, "Favorable absorption profile")
        } else if (lipinskiPasses || veberPasses) {
            AdmetResult("Moderate Bioavailability", true, "Acceptable oral absorption")
        } else {
            AdmetResult("Low Bioavailability (Risk)", false, "Poor oral absorption predicted")
        }

        // Blood-Brain Barrier (BBB) Penetration
        // High BBB penetration if LogP > 1.5, MW < 400, TPSA < 90
        val bbbPenetration = if (tpsa < 90.0 && logP > 1.0 && mw < 450.0) {
            AdmetResult("High BBB Crossing (CNS Target)", true, "Crosses blood-brain barrier efficiently")
        } else if (tpsa < 120.0 && mw < 500.0) {
            AdmetResult("Moderate BBB Crossing", true, "Limited CNS exposure")
        } else {
            AdmetResult("Low BBB Penetration (Non-CNS Safe)", false, "Low risk of CNS side effects")
        }

        // hERG Potassium Channel Inhibition (Cardiotoxicity Risk)
        // High risk if LogP > 3.5 and basic nitrogen / MW > 350
        val hergInhibition = if (logP > 3.8 && mw > 400.0 && nCount > 1) {
            AdmetResult("High Cardiotoxicity Risk (hERG)", false, "Potential QT prolongation risk")
        } else if (logP > 2.5 && mw > 300.0) {
            AdmetResult("Moderate hERG Risk", true, "Low-to-moderate hERG affinity")
        } else {
            AdmetResult("Low Cardiotoxicity Risk (Safe)", true, "Negligible hERG channel inhibition")
        }

        return Descriptors(
            mw = mw,
            logP = logP,
            hBondDonors = hbd,
            hBondAcceptors = hba,
            tpsa = tpsa,
            rotatableBonds = rotatableBonds,
            lipinskiPasses = lipinskiPasses,
            veberPasses = veberPasses,
            oralBioavailability = oralBioavailability,
            bbbPenetration = bbbPenetration,
            hergInhibition = hergInhibition
        )
    }
}
