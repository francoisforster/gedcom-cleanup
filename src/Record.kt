import java.io.Writer

private const val DATE_TAG = " DATE "
private const val PLACE_TAG = " PLAC "
const val CONT_TAG = " CONT "

/**
 * Generic nestable record which contains a line of text and sub records
 */
open class Record(var text: String) {
    val subRecords: MutableList<Record> = mutableListOf()

    fun addSubRecord(subRecord: Record) {
        subRecords.add(subRecord)
    }

    fun getSubRecord(tag: String): Record? {
        for (subRecord in subRecords) {
            if (subRecord.text.contains(tag)) {
                return subRecord
            }
        }
        return null
    }

    fun getSubRecordEndsWith(tag: String): Record? {
        for (subRecord in subRecords) {
            if (subRecord.text.trim().endsWith(tag)) {
                return subRecord
            }
        }
        return null
    }

    fun getSubRecordsEndsWith(tag: String): List<Record> {
        val records = mutableListOf<Record>()
        for (subRecord in subRecords) {
            if (subRecord.text.trim().endsWith(tag)) {
                records.add(subRecord)
            }
        }
        return records
    }

    fun getSubRecordReferences(tag: String): List<String> {
        val references = mutableListOf<String>()
        for (subRecord in subRecords) {
            if (subRecord.text.contains(tag)) {
                references.safeAdd(subRecord.getReference())
            }
        }
        return references
    }

    fun getSubRecordsText(tag: String): List<String> {
        val references = mutableListOf<String>()
        for (subRecord in subRecords) {
            if (subRecord.text.contains(tag)) {
                references.safeAdd(subRecord.text.substring(7))
            }
        }
        return references
    }

    fun removeSubRecord(tag: String) {
        subRecords.removeAll { subRecord -> subRecord.text.contains(tag) }
    }

    fun write(writer: Writer) {
        writer.write(text)
        writer.write("\r\n")
        for (subRecord in subRecords) {
            subRecord.write(writer)
        }
    }

    open fun matches(otherRecord: Record?): Boolean {
        return text == otherRecord?.text
    }

    protected fun matches(text1: String?, text2: String?): Boolean {
        if (text1 == null || text2 == null) {
            return text1 == null && text2 == null
        }
        return text1 == text2
    }

    fun getReferenceId(): String? {
        return getPartSafe(1)
    }

    fun setReferenceId(newId: String) {
        val parts = text.split(" ")
        text = parts[0] + " " + newId + " " + parts[2]
    }

    fun getReference(): String? {
        return getPartSafe(2)
    }

    private fun getPartSafe(part: Int): String? {
        val parts = text.split(" ")
        if (part >= parts.size) {
            return null
        }
        return parts[part]
    }

    fun parseEvent(parentReferenceId: String?): Event {
        var date: String? = null
        var place: String? = null
        var source: String? = null
        var note: String? = null
        for (subRecord in subRecords) {
            when {
                subRecord.text.contains(DATE_TAG) -> {
                    date = subRecord.text.substring(7)
                }
                subRecord.text.contains(PLACE_TAG) -> {
                    place = subRecord.text.substring(7)
                }
                subRecord.text.contains(SOURCE_TAG) -> {
                    source = subRecord.getReference()
                }
                subRecord.text.contains(NOTE_TAG) -> {
                    note = subRecord.getReference()
                }
            }
        }
        return Event(text.substring(2).trim(), parentReferenceId, this, date, place, source, note)
    }

    open fun clone(): Record {
        val clone = Record(text)
        cloneSubRecords(clone)
        return clone
    }

    protected fun cloneSubRecords(clone: Record) {
        for (subRecord in subRecords) {
            clone.addSubRecord(subRecord.clone())
        }
    }

}