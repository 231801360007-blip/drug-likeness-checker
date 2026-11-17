package biokt

// ============================================================
// BIOKT — A Bioinformatics Library for Kotlin (inspired by BioPython)
// ============================================================

// ─── Alphabets ───────────────────────────────────────────────

enum class Alphabet { DNA, RNA, PROTEIN, AMBIGUOUS_DNA, AMBIGUOUS_RNA }

object Alphabets {
    val DNA_BASES      = setOf('A', 'T', 'G', 'C')
    val RNA_BASES      = setOf('A', 'U', 'G', 'C')
    val AMINO_ACIDS    = setOf('A','C','D','E','F','G','H','I','K','L',
                                'M','N','P','Q','R','S','T','V','W','Y','*')
    val AMBIGUOUS_DNA  = DNA_BASES + setOf('N','R','Y','S','W','K','M','B','D','H','V')
    val AMBIGUOUS_RNA  = RNA_BASES + setOf('N','R','Y','S','W','K','M','B','D','H','V')

    fun validate(seq: String, alphabet: Alphabet): Boolean {
        val allowed = when (alphabet) {
            Alphabet.DNA            -> DNA_BASES
            Alphabet.RNA            -> RNA_BASES
            Alphabet.PROTEIN        -> AMINO_ACIDS
            Alphabet.AMBIGUOUS_DNA  -> AMBIGUOUS_DNA
            Alphabet.AMBIGUOUS_RNA  -> AMBIGUOUS_RNA
        }
        return seq.uppercase().all { it in allowed }
    }
}

// ─── Base Sequence ──────────────────────────────────────────

sealed class BioSequence(
    open val sequence: String,
    open val id: String = "",
    open val description: String = ""
) {
    val length: Int get() = sequence.length

    operator fun get(index: Int): Char = sequence[index]
    operator fun get(range: IntRange): String = sequence.substring(range)

    fun count(base: Char): Int = sequence.uppercase().count { it == base.uppercaseChar() }

    fun countAll(): Map<Char, Int> =
        sequence.uppercase().groupingBy { it }.eachCount()

    override fun toString(): String = sequence

    // Motif search — returns all start indices of pattern
    fun findMotif(pattern: String): List<Int> {
        val results = mutableListOf<Int>()
        val seq = sequence.uppercase()
        val pat = pattern.uppercase()
        var idx = 0
        while (true) {
            val found = seq.indexOf(pat, idx)
            if (found == -1) break
            results += found
            idx = found + 1
        }
        return results
    }

    // GC content (DNA/RNA)
    fun gcContent(): Double {
        if (length == 0) return 0.0
        val gc = sequence.uppercase().count { it == 'G' || it == 'C' }
        return gc.toDouble() / length * 100.0
    }

    // GC skew in a window
    fun gcSkew(windowSize: Int = 100, stepSize: Int = 10): List<Pair<Int, Double>> {
        val results = mutableListOf<Pair<Int, Double>>()
        val seq = sequence.uppercase()
        var pos = 0
        while (pos + windowSize <= seq.length) {
            val window = seq.substring(pos, pos + windowSize)
            val g = window.count { it == 'G' }.toDouble()
            val c = window.count { it == 'C' }.toDouble()
            val skew = if (g + c == 0.0) 0.0 else (g - c) / (g + c)
            results += Pair(pos, skew)
            pos += stepSize
        }
        return results
    }

    abstract fun complement(): BioSequence
    abstract fun reverseComplement(): BioSequence
}

// ─── DNA Sequence ───────────────────────────────────────────

class DNASequence(
    sequence: String,
    id: String = "",
    description: String = ""
) : BioSequence(sequence.uppercase(), id, description) {

    init {
        require(Alphabets.validate(sequence, Alphabet.AMBIGUOUS_DNA)) {
            "Invalid DNA sequence: contains non-DNA characters in '$sequence'"
        }
    }

    companion object {
        private val COMPLEMENT_MAP = mapOf(
            'A' to 'T', 'T' to 'A', 'G' to 'C', 'C' to 'G',
            'R' to 'Y', 'Y' to 'R', 'S' to 'S', 'W' to 'W',
            'K' to 'M', 'M' to 'K', 'B' to 'V', 'V' to 'B',
            'D' to 'H', 'H' to 'D', 'N' to 'N'
        )
    }

    override fun complement(): DNASequence {
        val comp = sequence.map { COMPLEMENT_MAP[it] ?: it }.joinToString("")
        return DNASequence(comp, id, description)
    }

    override fun reverseComplement(): DNASequence =
        DNASequence(complement().sequence.reversed(), id, description)

    fun transcribe(): RNASequence =
        RNASequence(sequence.replace('T', 'U'), id, description)

    // Translate DNA directly to protein
    fun translate(
        table: CodonTable = CodonTables.STANDARD,
        toStop: Boolean = true,
        startCodon: Boolean = false
    ): ProteinSequence {
        val seq = if (startCodon) {
            val firstStart = sequence.indexOf("ATG")
            if (firstStart == -1) "" else sequence.substring(firstStart)
        } else sequence

        val aaList = mutableListOf<Char>()
        var i = 0
        while (i + 3 <= seq.length) {
            val codon = seq.substring(i, i + 3)
            val aa = try { table.translate(codon) } catch (e: Exception) { '?' }
            if (toStop && aa == '*') break
            aaList.add(aa)
            i += 3
        }
        return ProteinSequence(aaList.joinToString(""), id, description)
    }

    // Six-frame translation
    fun sixFrameTranslation(table: CodonTable = CodonTables.STANDARD): Map<Int, ProteinSequence> {
        val frames = mutableMapOf<Int, ProteinSequence>()
        for (frame in 0..2) {
            val fSeq = sequence.substring(frame)
            frames[frame + 1] = DNASequence(fSeq).translate(table, toStop = false)
        }
        val rc = reverseComplement().sequence
        for (frame in 0..2) {
            val fSeq = rc.substring(frame)
            frames[-(frame + 1)] = DNASequence(fSeq).translate(table, toStop = false)
        }
        return frames
    }

    // Find Open Reading Frames
    fun findOrfs(
        table: CodonTable = CodonTables.STANDARD,
        minLength: Int = 30   // min length in amino acids
    ): List<Orf> {
        val orfs = mutableListOf<Orf>()
        val frames = sixFrameTranslation(table)
        for ((frame, prot) in frames) {
            val pStr = prot.sequence
            var start = -1
            for (i in pStr.indices) {
                if (pStr[i] == 'M' && start == -1) start = i
                else if (pStr[i] == '*' && start != -1) {
                    val len = i - start
                    if (len >= minLength) {
                        val nucStart = if (frame > 0) (frame - 1) + start * 3
                                       else length - ((abs(frame) - 1) + (i + 1) * 3)
                        val nucEnd = nucStart + (len + 1) * 3
                        orfs.add(Orf(frame, start, i, len, nucStart, nucEnd, pStr.substring(start, i)))
                    }
                    start = -1
                }
            }
        }
        return orfs.sortedByDescending { it.length }
    }

    // Melting temperature calculation
    fun meltingTemperature(): Double {
        val a = count('A'); val t = count('T')
        val g = count('G'); val c = count('C')
        return if (length < 14)
            (a + t) * 2.0 + (g + c) * 4.0
        else
            64.9 + 41.0 * (g + c - 16.4) / length
    }

    fun subSequence(start: Int, end: Int): DNASequence =
        DNASequence(sequence.substring(start, end), id, description)
}

data class Orf(
    val frame: Int,
    val startAa: Int,
    val endAa: Int,
    val length: Int,        // in amino acids
    val nucStart: Int,
    val nucEnd: Int,
    val proteinSequence: String
)

// ─── RNA Sequence ───────────────────────────────────────────

class RNASequence(
    sequence: String,
    id: String = "",
    description: String = ""
) : BioSequence(sequence.uppercase(), id, description) {

    init {
        require(Alphabets.validate(sequence, Alphabet.AMBIGUOUS_RNA)) {
            "Invalid RNA sequence: contains non-RNA characters in '$sequence'"
        }
    }

    companion object {
        private val COMPLEMENT_MAP = mapOf(
            'A' to 'U', 'U' to 'A', 'G' to 'C', 'C' to 'G'
        )
    }

    override fun complement(): RNASequence {
        val comp = sequence.map { COMPLEMENT_MAP[it] ?: it }.joinToString("")
        return RNASequence(comp, id, description)
    }

    override fun reverseComplement(): RNASequence =
        RNASequence(complement().sequence.reversed(), id, description)

    fun backTranscribe(): DNASequence =
        DNASequence(sequence.replace('U', 'T'), id, description)

    fun translate(table: CodonTable = CodonTables.STANDARD): ProteinSequence {
        val dna = backTranscribe()
        return dna.translate(table)
    }
}

// ─── Protein Sequence ───────────────────────────────────────

class ProteinSequence(
    sequence: String,
    id: String = "",
    description: String = ""
) : BioSequence(sequence.uppercase(), id, description) {

    override fun complement(): ProteinSequence =
        throw UnsupportedOperationException("Complement not defined for protein sequences")

    override fun reverseComplement(): ProteinSequence =
        throw UnsupportedOperationException("Reverse complement not defined for protein sequences")

    fun molecularWeight(): Double {
        val weights = mapOf(
            'A' to 89.09, 'R' to 174.20, 'N' to 132.12, 'D' to 133.10, 'C' to 121.16,
            'E' to 147.13, 'Q' to 146.15, 'G' to 75.07, 'H' to 155.16, 'I' to 131.17,
            'L' to 131.17, 'K' to 146.19, 'M' to 149.21, 'F' to 165.19, 'P' to 115.13,
            'S' to 105.09, 'T' to 119.12, 'W' to 204.23, 'Y' to 181.19, 'V' to 117.15
        )
        var mw = 0.0
        for (aa in sequence) mw += (weights[aa] ?: 0.0) - 18.015  // subtract H2O per peptide bond
        return if (length > 0) mw + 18.015 else 0.0
    }

    fun isoelectricPoint(): Double {
        // Simplified pI calculation using iterative bisection
        var low = 0.0; var high = 14.0
        while (high - low > 0.01) {
            val mid = (low + high) / 2
            val charge = chargeAtPh(mid)
            if (charge > 0) low = mid else high = mid
        }
        return (low + high) / 2
    }

    fun chargeAtPh(ph: Double): Double {
        fun pKa(base: Double) = 1.0 / (1.0 + Math.pow(10.0, ph - base))
        fun pKb(base: Double) = 1.0 / (1.0 + Math.pow(10.0, base - ph))

        var charge = pKb(9.69) - pKa(2.34)   // N- and C-terminus
        charge += count('K') * pKb(10.53)
        charge += count('R') * pKb(12.48)
        charge += count('H') * pKb(6.00)
        charge -= count('D') * pKa(3.86)
        charge -= count('E') * pKa(4.25)
        charge -= count('C') * pKa(8.33)
        charge -= count('Y') * pKa(10.07)
        return charge
    }

    fun aromaticity(): Double {
        if (length == 0) return 0.0
        val arom = count('F') + count('W') + count('Y')
        return arom.toDouble() / length
    }

    fun instabilityIndex(): Double {
        // Simplified instability index estimate
        var score = 0.0
        for (i in 0 until length - 1) {
            val di = "${sequence[i]}${sequence[i+1]}"
            if (di in setOf("KL", "KK", "RR", "KR")) score += 1.0
        }
        return if (length == 0) 0.0 else score / length * 100.0
    }
}

private fun abs(n: Int): Int = if (n < 0) -n else n
