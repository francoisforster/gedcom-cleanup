const val PARENT_FAMILY_TAG = " FAMC "
const val SPOUSE_FAMILY_TAG = " FAMS "
const val BIRTH_TAG = " BIRT"
const val DEATH_TAG = " DEAT"
const val CENSUS_TAG = " CENS"
const val RESIDENCE_TAG = " RESI"
const val NAME_TAG = " NAME "
const val GENDER_TAG = " SEX "

/**
 * Specific record for individuals
 */
class IndividualRecord(record: Record) : Record(record.text) {

    /**
     * Returns the parent family reference id
     */
    fun getParentFamily(): String? {
        return getSubRecord(PARENT_FAMILY_TAG)?.getReference()
    }

    fun removeParentFamily() {
        removeSubRecord(PARENT_FAMILY_TAG)
    }

    fun addParentFamily(familyRecord: FamilyRecord) {
        addSubRecord(Record("1${PARENT_FAMILY_TAG}${familyRecord.getReferenceId()}"))
    }

    /**
     * Returns a list of spouse family reference ids
     */
    fun getFamilies(): List<String> {
        return getSubRecordReferences(SPOUSE_FAMILY_TAG)
    }

    fun removeFamilies() {
        removeSubRecord(SPOUSE_FAMILY_TAG)
    }

    fun addFamily(familyRecord: FamilyRecord) {
        addSubRecord(Record("1${SPOUSE_FAMILY_TAG}${familyRecord.getReferenceId()}"))
    }

    /**
     * Returns the birth event
     */
    fun getBirth(): Event? {
        return getSubRecordEndsWith(BIRTH_TAG)?.parseEvent(getReferenceId())
    }

    /**
     * Returns the death event
     */
    fun getDeath(): Event? {
        return getSubRecordEndsWith(DEATH_TAG)?.parseEvent(getReferenceId())
    }

    fun getCensus(): List<Event> {
        return getSubRecords(CENSUS_TAG)
    }

    fun getResidences(): List<Event> {
        return getSubRecords(RESIDENCE_TAG)
    }
	
    fun getSubRecords(type: String): List<Event> {
        val subRecords = mutableListOf<Event>()
        val records = getSubRecordsEndsWith(type)
        for (record in records) {
            subRecords.add(record.parseEvent(getReferenceId()))
        }
        return subRecords
    }

    fun getNote(): String? {
    	return getSubRecord(NOTE_TAG)?.getReference()
    }

    fun getSource(): String? {
    	return getSubRecord(SOURCE_TAG)?.getReference()
    }

    /**
     * Returns the name of the individual
     */
    fun getName(): String? {
        val parts = getNameParts()
        if (parts != null) {
            if (parts.first == "") {
                return parts.second
            } else if (parts.second == "") {
                return parts.first
            }
            return "${parts.first} ${parts.second}"
        }
        return null
    }

    fun getLastname(): String? {
        val parts = getNameParts()
        if (parts?.second == "") {
            return null
        }
        return parts?.second
    }

    fun getFirstname(): String? {
        val parts = getNameParts()
        return parts?.first
    }

    private fun getNameParts(): Pair<String?, String?>? {
        val name = getSubRecord(NAME_TAG)?.text?.substring(7)
        val parts = name?.split("/")
        if (parts != null && parts.size > 1) {
            val firstname = parts[0].trim()
            val lastname = parts[1].trim()
            return Pair(firstname, lastname)
        }
        return null
    }

    fun getGender(): Char? {
        return getSubRecord(GENDER_TAG)?.text?.substring(6, 7)?.getOrNull(0)
    }

    override fun clone(): IndividualRecord {
        val clone = IndividualRecord(this)
        cloneSubRecords(clone)
        return clone
    }
}


fun newIndividual(referenceId: String): IndividualRecord {
    return IndividualRecord(Record("0 $referenceId $INDIVIDUAL_REFERENCE_TYPE"))
}
