package biokt

/**
 * Represents a chemical molecule constructed from SMILES string and an optional drug name.
 */
data class Molecule(
    val smiles: String,
    val name: String = ""
) {
    val length: Int get() = smiles.length

    override fun toString(): String {
        return "Molecule(name='$name', smiles='$smiles')"
    }
}
