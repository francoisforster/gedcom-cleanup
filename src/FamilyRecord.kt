private const val HUSBAND_TAG = " HUSB "
private const val WIFE_TAG = " WIFE "
private const val CHILD_TAG = " CHIL "

/**
 * Specific record for families
 */
class FamilyRecord(record: Record) : Record(record.text) {
    fun getHusband(): String? {
        for (subRecord in subRecords) {
            if (subRecord.text.contains(HUSBAND_TAG)) {
                return subRecord.text.substring(7)
            }
        }
        return null
    }

    fun getWife(): String? {
        for (subRecord in subRecords) {
            if (subRecord.text.contains(WIFE_TAG)) {
                return subRecord.text.substring(7)
            }
        }
        return null
    }

    fun getChildren(): List<String> {
        val children = mutableListOf<String>()
        for (subRecord in subRecords) {
            if (subRecord.text.contains(CHILD_TAG)) {
                children.add(subRecord.text.substring(7))
            }
        }
        return children
    }
}
