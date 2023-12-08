import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.DateTimeParseException
import java.util.Locale

private val shortFormatter = getFormatter("d MMM yyyy")
private val longFormatter = getFormatter("d MMMM yyyy")

fun getFormatter(pattern: String): DateTimeFormatter {
    val builder = DateTimeFormatterBuilder()
    return builder.parseCaseInsensitive()
            .appendPattern(pattern)
            .toFormatter()
            .withLocale(Locale.US)
}

data class Event(
    val type: String?,
    val parentReferenceId: String?,
    val record: Record,
    val date: String?,
    val place: String?,
    val source: String?,
    val note: String?
) {
    fun getYear(): Int? {
        return date?.substring(date.lastIndexOf(" ") + 1)?.toInt()
    }

    override fun toString(): String {
        return "Event(type=$type, date=$date, place=$place)"
    }

    fun matches(other: Event?): Boolean {
        return datesMatch(date, other?.date) && getShortPlace() == other?.getShortPlace()
    }
	
	fun parseDate(): LocalDate? {
		if (date == null) {
			return null
		}
        return parseDate(date)
    }
	
	fun getShortPlace(): String? {
        var shortPlace = place
        if (place?.contains(',') == true) {
            shortPlace = place.substring(0, place.indexOf(","))
        }
		return shortPlace
	}
	
    private fun datesMatch(date: String?, otherDate: String?): Boolean {
        if (date == null || otherDate == null) {
            return date == otherDate
        }
        return try {
            parseDate(date) == parseDate(otherDate)
        } catch (e: DateTimeParseException) {
            date == otherDate
        }
    }

    private fun parseDate(date: String): LocalDate {
        return try {
            LocalDate.parse(date, shortFormatter)
        } catch (e: DateTimeParseException) {
            LocalDate.parse(date, longFormatter)
        }
    }
}
