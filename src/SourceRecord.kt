private const val TITLE_TAG = " TITL "

private const val TEXT_TAG = " TEXT "

/**
 * Specific record for sources
 */
class SourceRecord(record: Record) : Record(record.text) {

    override fun matches(otherRecord: Record?): Boolean {
        return matches(getSubRecord(TITLE_TAG)?.text, otherRecord?.getSubRecord(TITLE_TAG)?.text) &&
                matches(getSubRecord(TEXT_TAG)?.text, otherRecord?.getSubRecord(TEXT_TAG)?.text)
    }
}
