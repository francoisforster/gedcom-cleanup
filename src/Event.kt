data class Event(
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
        return "Event(date=$date, place=$place)"
    }

    fun matches(other: Event?): Boolean {
        var shortPlace = place
        if (place?.contains(',') == true) {
            shortPlace = place?.substring(0, place.indexOf(","))
        }
        var otherShortPlace = other?.place
        if (other?.place?.contains(',') == true) {
            otherShortPlace = other?.place?.substring(0, other?.place.indexOf(","))
        }
        return date == other?.date && shortPlace == otherShortPlace
    }
}
