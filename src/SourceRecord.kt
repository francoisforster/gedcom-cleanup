private const val TITLE_TAG = " TITL "
private const val TEXT_TAG = " TEXT "

/**
 * Specific record for sources
 */
class SourceRecord(record: Record) : Record(record.text) {

    fun getSourceTitle(): String? {
        return getSubRecord(TITLE_TAG)?.text
    }

    fun getSourceText(): String? {
        return getSubRecord(TEXT_TAG)?.text
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
}
