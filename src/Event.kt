data class Event(val parentReferenceId: String?, val date: String?, val place: String?, val source: String?) {
    fun getYear(): Int? {
        return date?.substring(date.lastIndexOf(" ") + 1)?.toInt()
    }
}
