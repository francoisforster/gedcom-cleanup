private const val HUSBAND_TAG = " HUSB "
private const val WIFE_TAG = " WIFE "
private const val CHILD_TAG = " CHIL "
private const val MARRIAGE_TAG = " MARR"

/**
 * Specific record for families
 */
class FamilyRecord(record: Record) : Record(record.text) {

    /**
     * Returns the reference id of the husband of the family
     */
    fun getHusband(): String? {
        return getSubRecord(HUSBAND_TAG)?.getReference()
    }

    /**
     * Returns the reference id of the wife of the family
     */
    fun getWife(): String? {
        return getSubRecord(WIFE_TAG)?.getReference()
    }

    /**
     * Returns a list of reference ids of the children of the family
     */
    fun getChildren(): List<String> {
        return getSubRecordReferences(CHILD_TAG)
    }

    /**
     * Returns the marriage event
     */
    fun getMarriage(): Event? {
        return getSubRecord(MARRIAGE_TAG)?.parseEvent(getReferenceId())
    }

}
