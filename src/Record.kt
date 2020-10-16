import java.io.Writer

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

    fun matches(text1: String?, text2: String?): Boolean {
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

}