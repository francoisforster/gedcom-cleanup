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
        return getSubRecord(PARENT_FAMILY_TAG)?.getReference()
    }

    /**
     * Returns a list of spouse family reference ids
     */
    fun getFamilies(): List<String> {
        return getSubRecordReferences(SPOUSE_FAMILY_TAG)
    }

    /**
     * Returns the birth event
     */
    fun getBirth(): Event? {
        return getSubRecord(BIRTH_TAG)?.parseEvent(getReferenceId())
    }

    /**
     * Returns the death event
     */
    fun getDeath(): Event? {
        return getSubRecord(DEATH_TAG)?.parseEvent(getReferenceId())
    }

}
