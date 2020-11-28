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
        val shortPlace = place?.substring(0, place.indexOf(","))
        val otherShortPlace = other?.place?.substring(0, other.place.indexOf(","))
        return date == other?.date && shortPlace == otherShortPlace
    }
}
