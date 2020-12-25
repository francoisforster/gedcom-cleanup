/**
 * Specific record for notes
 */
class NoteRecord(record: Record) : Record(record.text) {

    fun getNoteText(): String {
        return text.substring(7)
    }

    fun getNoteContinuation(): List<String> {
        return getSubRecordsText(CONT_TAG)
    }

    /**
     * Note text is part of the note definition line
     */
    override fun matches(otherRecord: Record?): Boolean {
        return otherRecord is NoteRecord
                && getNoteText() == otherRecord.getNoteText()
                && getNoteContinuation() == otherRecord.getNoteContinuation()
    }

    override fun clone(): NoteRecord {
        val clone = NoteRecord(this)
        cloneSubRecords(clone)
        return clone
    }
}
