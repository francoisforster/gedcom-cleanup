const val TITLE_TAG = " TITL "
const val TEXT_TAG = " TEXT "

/**
 * Specific record for sources
 */
class SourceRecord(record: Record) : Record(record.text) {

    fun getSourceTitle(): String? {
        return getSubRecord(TITLE_TAG)?.text?.substring(7)
    }

    fun getSourceText(): String? {
        return getSubRecord(TEXT_TAG)?.text?.substring(7)
    }

    fun getSourceContinuation(): List<String>? {
        return getSubRecord(TEXT_TAG)?.getSubRecordsText(CONT_TAG)
    }

    override fun matches(otherRecord: Record?): Boolean {
        return otherRecord is SourceRecord
                && matches(getSourceTitle(), otherRecord.getSourceTitle())
                && matches(getSourceText(), otherRecord.getSourceText())
                && getSourceContinuation() == otherRecord.getSourceContinuation()
    }

    override fun clone(): SourceRecord {
        val clone = SourceRecord(this)
        cloneSubRecords(clone)
        return clone
    }
	
	override fun toString(): String {
        return "Source(title=${getSourceTitle()}, text=${getSourceText()}, continuation=${getSourceContinuation()})"
    }

}
