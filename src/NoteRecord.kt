/**
 * Specific record for notes
 */
class NoteRecord(record: Record) : Record(record.text) {

    /**
     * Note text is part of the note definition text
     */
    override fun matches(otherRecord: Record?): Boolean {
        return text.substring(7) == otherRecord?.text?.substring(7)
    }

}
