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
        for (subRecord in subRecords) {
            if (subRecord.text.contains(HUSBAND_TAG)) {
                return subRecord.getReference()
            }
        }
        return null
    }

    /**
     * Returns the reference id of the wife of the family
     */
    fun getWife(): String? {
        for (subRecord in subRecords) {
            if (subRecord.text.contains(WIFE_TAG)) {
                return subRecord.getReference()
            }
        }
        return null
    }

    /**
     * Returns a list of reference ids of the children of the family
     */
    fun getChildren(): List<String> {
        val children = mutableListOf<String>()
        for (subRecord in subRecords) {
            if (subRecord.text.contains(CHILD_TAG)) {
                children.safeAdd(subRecord.getReference())
            }
        }
        return children
    }

    /**
     * Returns the marriage event
     */
    fun getMarriage(): Event? {
        for (subRecord in subRecords) {
            if (subRecord.text.contains(MARRIAGE_TAG)) {
                return subRecord.parseEvent(getReferenceId())
            }
        }
        return null
    }

}
