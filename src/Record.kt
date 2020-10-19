import java.io.Writer

private const val DATE_TAG = " DATE "
private const val PLACE_TAG = " PLAC "

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
            }
        }
        return Event(parentReferenceId, date, place, source)
    }

}