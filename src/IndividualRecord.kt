private const val PARENT_FAMILY_TAG = " FAMC "
private const val SPOUSE_FAMILY_TAG = " FAMS "
private const val BIRTH_TAG = " BIRT"
private const val DEATH_TAG = " DEAT"

/**
 * Specific record for individuals
 */
class IndividualRecord(record: Record) : Record(record.text) {

    /**
     * Returns the parent family reference id
     */
    fun getParentFamily(): String? {
        for (subRecord in subRecords) {
            if (subRecord.text.contains(PARENT_FAMILY_TAG)) {
                return subRecord.text.substring(7)
            }
        }
        return null
    }

    /**
     * Returns a list of spouse family reference ids
     */
    fun getFamilies(): List<String> {
        val families = mutableListOf<String>()
        for (subRecord in subRecords) {
            if (subRecord.text.contains(SPOUSE_FAMILY_TAG)) {
                families.add(subRecord.text.substring(7))
            }
        }
        return families
    }

    /**
     * Returns the birth event
     */
    fun getBirth(): Event? {
        for (subRecord in subRecords) {
            if (subRecord.text.contains(BIRTH_TAG)) {
                return subRecord.parseEvent(getReferenceId())
            }
        }
        return null
    }

    /**
     * Returns the death event
     */
    fun getDeath(): Event? {
        for (subRecord in subRecords) {
            if (subRecord.text.contains(DEATH_TAG)) {
                return subRecord.parseEvent(getReferenceId())
            }
        }
        return null
    }

}
