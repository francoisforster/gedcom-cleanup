import java.io.BufferedReader
import java.io.FileReader
import java.io.Writer
import java.util.*

const val TRAILER = "0 TRLR"

const val SOURCE_TAG = " SOUR "
const val SOURCE_REFERENCE_PREFIX = "@S"
const val SOURCE_REFERENCE_TYPE = "SOUR"

const val NOTE_TAG = " NOTE "
const val NOTE_REFERENCE_PREFIX = "@N"
const val NOTE_REFERENCE_TYPE = "NOTE"

const val INDIVIDUAL_REFERENCE_PREFIX = "@I"
const val INDIVIDUAL_REFERENCE_TYPE = "INDI"

const val FAMILY_REFERENCE_PREFIX = "@F"
const val FAMILY_REFERENCE_TYPE = "FAM"

private const val utf8BOM = "\u00ef\u00bb\u00bf"
private const val utf16BOM = "\uFEFF"

fun <T> MutableCollection<T>.safeAdd(element: T?) {
    if (element != null) {
        add(element)
    }
}

fun <T> MutableCollection<T>.safeAddAll(elements: Collection<T>?) {
    if (elements != null) {
        addAll(elements)
    }
}

/**
 * Class that represents gedcom data
 */
class Gedcom {

    private val records = mutableMapOf<String, Record>()

    private val individualIds = mutableSetOf<Long>()
    private val familyIds = mutableSetOf<Long>()
    private val noteIds = mutableSetOf<Long>()
    private val sourceIds = mutableSetOf<Long>()

    /**
     * Parse a GEDCOM file. Doesn't validate the GEDCOM format.
     */
    fun parseFile(filename: String) {
        records.clear()
        val br = BufferedReader(FileReader(filename))
        val recordStack = Stack<Record>()
        var previousRecord: Record? = null
        var previousLevel = 0
        var line = br.readLine()
        while (line != null) {
            if (line.startsWith(utf8BOM)) {
                line = line.substring(3)
            }
            if (line.startsWith(utf16BOM)) {
                line = line.substring(1)
            }
            val level = Integer.parseInt(line.substring(0, 1))
            var record = Record(line)
            if (level == 0) {
                val reference = record.getReferenceId()
                if (reference != null) {
                    record = when (record.getReference()) {
                        NOTE_REFERENCE_TYPE -> {
                            noteIds.add(parseReferenceId(reference))
                            NoteRecord(record)
                        }
                        SOURCE_REFERENCE_TYPE -> {
                            sourceIds.add(parseReferenceId(reference))
                            SourceRecord(record)
                        }
                        INDIVIDUAL_REFERENCE_TYPE -> {
                            individualIds.add(parseReferenceId(reference))
                            IndividualRecord(record)
                        }
                        FAMILY_REFERENCE_TYPE ->  {
                            familyIds.add(parseReferenceId(reference))
                            FamilyRecord(record)
                        }
                        else -> record
                    }
                    if (!line.startsWith(TRAILER)) {
                        records[reference] = record
                    }
                }
                recordStack.empty()
            } else {
                while (level <= previousLevel) {
                    previousRecord = recordStack.pop()
                    previousLevel--
                }
                previousRecord?.addSubRecord(record)
            }
            recordStack.push(previousRecord)
            previousRecord = record
            previousLevel = level
            line = br.readLine()
        }
    }

    /**
     * Removes all individual and family records not reachable from the given starting individual
     */
    fun removeUnreachable(rootIndividual: IndividualRecord) {
        val traversedIndividuals = mutableSetOf<String>()
        val traversedFamilies = mutableSetOf<String>()
        traverse(rootIndividual, traversedIndividuals, traversedFamilies)
        var totalIndividualCount = 0
        var totalFamilyCount = 0
        val removeKeys = mutableSetOf<String>()
        for (key in records.keys) {
            if (key.startsWith(INDIVIDUAL_REFERENCE_PREFIX)) {
                totalIndividualCount++
                if (key !in traversedIndividuals) {
                    removeKeys.add(key)
                }
            } else if (key.startsWith(FAMILY_REFERENCE_PREFIX)) {
                totalFamilyCount++
                if (key !in traversedFamilies) {
                    removeKeys.add(key)
                }
            }
        }
        for (key in removeKeys) {
            records.remove(key)
        }
        println("$totalIndividualCount INDI found")
        println("${traversedIndividuals.size} INDI traversed")
        println("${totalIndividualCount - traversedIndividuals.size} INDI removed")
        println("$totalFamilyCount FAM found")
        println("${traversedFamilies.size} FAM traversed")
        println("${totalFamilyCount - traversedFamilies.size} FAM removed")
    }

    private fun traverse(
        individual: IndividualRecord?,
        traversedIndividuals: MutableSet<String>,
        traversedFamilies: MutableSet<String>
    ) {
        val referenceId = individual?.getReferenceId()
        if (referenceId != null && referenceId !in traversedIndividuals) {
            traversedIndividuals.add(referenceId)
            val parentsReference = individual.getParentFamily()
            val parents = getFamily(parentsReference)
            for (parent in listOf(parents?.getHusband(), parents?.getWife())) {
                traverse(getIndividual(parent), traversedIndividuals, traversedFamilies)
            }

            for (familyReference in individual.getFamilies()) {
                val family = getFamily(familyReference)
                val familyReferenceId = family?.getReferenceId()
                if (familyReferenceId != null && familyReferenceId !in traversedFamilies) {
                    traversedFamilies.add(familyReferenceId)
                    traverse(getIndividual(family.getHusband()), traversedIndividuals, traversedFamilies)
                    traverse(getIndividual(family.getWife()), traversedIndividuals, traversedFamilies)
                    for (child in family.getChildren()) {
                        traverse(getIndividual(child), traversedIndividuals, traversedFamilies)
                    }
                }
            }
        }
    }

    /**
     * Canonicalizes, dedupes and removes unreachable records such as Notes and Sources
     */
    fun cleanUpReferences(tag: String, referencePrefix: String) {
        val canonicalReferences = generateCanonicalReferences(referencePrefix)
        val allReferences = mutableSetOf<String>()
        val traversedReferences = mutableSetOf<String>()
        for (key in records.keys) {
            if (key.startsWith(referencePrefix)) {
                allReferences.add(key)
            } else {
                canonicalizeReferences(tag, records[key], traversedReferences, canonicalReferences)
            }
        }

        val uniqueCanonicalReferences = HashSet<String>(canonicalReferences.values)
        println("${allReferences.size} $tag found")
        println("${uniqueCanonicalReferences.size} $tag canonical values")
        println("${traversedReferences.size} $tag traversed")
        var removed = 0
        for (key in allReferences) {
            if (!traversedReferences.contains(key)) {
                records.remove(key)
                removed++
            }
        }
        println("$removed $tag removed")

    }

    private fun generateCanonicalReferences(referencePrefix: String): Map<String, String> {
        val canonicalReferences = mutableMapOf<String, String>()
        for (key in records.keys) {
            if (key.startsWith(referencePrefix)) {
                canonicalReferences[key] = getCanonicalKey(key, canonicalReferences)
            }
        }
        return canonicalReferences
    }

    private fun getCanonicalKey(key: String, canonicalReferences: Map<String, String>): String {
        for (canonicalKey in canonicalReferences.values) {
            if (records[key]?.matches(records[canonicalKey]) == true) {
                return canonicalKey
            }
        }
        return key
    }

    private fun canonicalizeReferences(
        tag: String,
        record: Record?,
        traversedReferences: MutableSet<String>,
        canonicalReferences: Map<String, String>
    ) {
        if (record != null) {
            if (record.text.contains(tag)) {
                val id = record.getReference()
                val canonicalKey = canonicalReferences[id]
                if (canonicalKey != null) {
                    record.text = record.text.substring(0, 7) + canonicalKey
                    traversedReferences.add(canonicalKey)
                }
            }
            for (subRecord in record.subRecords) {
                canonicalizeReferences(tag, subRecord, traversedReferences, canonicalReferences)
            }
        }
    }

    /**
     * Validates that individual and family record events that match the given criteria are valid according
     * to the given criteria
     */
    fun validateEvents(selectionCriteria: (Event) -> Boolean, validationCriteria: (Event, Gedcom) -> Boolean) {
        val allEvents = mutableListOf<Event>()
        for (key in records.keys) {
            if (key.startsWith(INDIVIDUAL_REFERENCE_PREFIX)) {
                val individual = getIndividual(key)
                allEvents.safeAdd(individual?.getBirth())
                allEvents.safeAdd(individual?.getDeath())
                allEvents.safeAddAll(individual?.getCensus())
            } else if (key.startsWith(FAMILY_REFERENCE_PREFIX)) {
                val family = getFamily(key)
                allEvents.safeAdd(family?.getMarriage())
            }
        }
        var matchedEventCount = 0
        var failedValidationCount = 0
        for (event in allEvents) {
            if (selectionCriteria.invoke(event)) {
                matchedEventCount++
                if (!validationCriteria.invoke(event, this)) {
                    println("Validation failed for ${event.parentReferenceId}")
                    failedValidationCount++
                }
            }
        }
        println("${allEvents.size} events found")
        println("$matchedEventCount events matched criteria")
        println("$failedValidationCount events failed validation")
    }


    /**
     * Gets a specific individual by reference id
     */
    fun getIndividual(referenceId: String?): IndividualRecord? {
        return getRecord(referenceId)
    }

    fun addIndividual(referenceId: String, individual: IndividualRecord) {
        records[referenceId] = individual
        individualIds.add(parseReferenceId(referenceId))
    }

    /**
     * Gets a specific family by reference id
     */
    fun getFamily(referenceId: String?): FamilyRecord? {
        return getRecord(referenceId)
    }

    fun addFamily(referenceId: String, family: FamilyRecord) {
        records[referenceId] = family
        familyIds.add(parseReferenceId(referenceId))
    }

    /**
     * Gets a specific source by reference id
     */
    fun getSource(referenceId: String?): SourceRecord? {
        return getRecord(referenceId)
    }

    fun addSource(referenceId: String, source: SourceRecord) {
        records[referenceId] = source
        sourceIds.add(parseReferenceId(referenceId))
    }

    /**
     * Gets a specific note by reference id
     */
    fun getNote(referenceId: String?): NoteRecord? {
        return getRecord(referenceId)
    }

    fun addNote(referenceId: String, note: NoteRecord) {
        records[referenceId] = note
        sourceIds.add(parseReferenceId(referenceId))
    }

    private inline fun <reified T : Record> getRecord(referenceId: String?): T? {
        if (referenceId in records) {
            val record = records[referenceId]
            if (record is T) {
                return record
            }
        }
        return null
    }

    fun getRecords(): Map<String, Record> {
        return records
    }


    /**
     * Saves the GEDCOM records to a file
     */
    fun write(writer: Writer) {
        writer.write(utf8BOM)
        for (record: Record in records.values) {
            record.write(writer)
        }
        writer.write(TRAILER)
        writer.write("\r\n")
    }

    fun generateIndividualReferenceId(): String {
        return generateReferenceId(individualIds, INDIVIDUAL_REFERENCE_PREFIX)
    }

    fun generateFamilyReferenceId(): String {
        return generateReferenceId(familyIds, FAMILY_REFERENCE_PREFIX)
    }

    fun generateNoteReferenceId(): String {
        return generateReferenceId(noteIds, NOTE_REFERENCE_PREFIX)
    }

    fun generateSourceReferenceId(): String {
        return generateReferenceId(sourceIds, SOURCE_REFERENCE_PREFIX)
    }

    private fun generateReferenceId(ids: MutableSet<Long>, prefix: String): String {
        var id: Long = 1
        while (ids.contains(id)) {
            id++
        }
        return "${prefix}${id}@"
    }

    private fun parseReferenceId(referenceIdStr: String): Long {
        return referenceIdStr.substring(2, referenceIdStr.length - 1).toLong()
    }
}