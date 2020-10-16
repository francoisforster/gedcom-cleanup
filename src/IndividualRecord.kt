private const val PARENT_FAMILY_TAG = " FAMC "
private const val SPOUSE_FAMILY_TAG = " FAMS "

/**
 * Specific record for individuals
 */
class IndividualRecord(record: Record) : Record(record.text) {

    fun getParentFamily(): String? {
        for (subRecord in subRecords) {
            if (subRecord.text.contains(PARENT_FAMILY_TAG)) {
                return subRecord.text.substring(7)
            }
        }
        return null
    }

    fun getFamilies(): List<String> {
        val families = mutableListOf<String>()
        for (subRecord in subRecords) {
            if (subRecord.text.contains(SPOUSE_FAMILY_TAG)) {
                families.add(subRecord.text.substring(7))
            }
        }
        return families
    }
}
