private const val FAMILY_TAG = "FAM"
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

    fun setHusband(referenceId: String) {
        setOrAdd(HUSBAND_TAG, referenceId)
    }

    /**
     * Returns the reference id of the wife of the family
     */
    fun getWife(): String? {
        return getSubRecord(WIFE_TAG)?.getReference()
    }

    fun setWife(referenceId: String) {
        setOrAdd(WIFE_TAG, referenceId)
    }

    private fun setOrAdd(tag: String, referenceId: String) {
        val subRecord = getSubRecord(tag)
        if (subRecord == null) {
            addSubRecord(Record("1${tag}${referenceId}"))
        } else {
            subRecord.setReferenceId(referenceId)
        }
    }

    /**
     * Returns a list of reference ids of the children of the family
     */
    fun getChildren(): List<String> {
        return getSubRecordReferences(CHILD_TAG)
    }

    fun addChild(referenceId: String) {
        addSubRecord(Record("1${CHILD_TAG}${referenceId}"))
    }

    /**
     * Returns the marriage event
     */
    fun getMarriage(): Event? {
        return getSubRecordEndsWith(MARRIAGE_TAG)?.parseEvent(getReferenceId())
    }
}

fun newFamily(referenceId: String): FamilyRecord {
    return FamilyRecord(Record("0 $referenceId $FAMILY_TAG"))
}

