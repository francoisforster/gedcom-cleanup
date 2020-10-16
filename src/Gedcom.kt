import java.io.BufferedReader
import java.io.FileReader
import java.io.Writer
import java.util.*

const val SOURCE_TAG = " SOUR "
const val SOURCE_REFERENCE_PREFIX = "@S"

const val NOTE_TAG = " NOTE "
const val NOTE_REFERENCE_PREFIX = "@N"

const val INDIVIDUAL_REFERENCE_PREFIX = "@I"

const val FAMILY_REFERENCE_PREFIX = "@F"

private const val utf8BOM = "\uFEFF"

/**
 * Class that represents gedcom data
 */
class Gedcom {

    private val records: MutableMap<String, Record> = mutableMapOf()

    /**
     * Parse a GEDCOM file. Doesn't validate the GEDCOM format.
     */
    fun parseFile(filename: String) {
        records.clear()
        val br = BufferedReader(FileReader(filename))
        val recordStack: Stack<Record> = Stack()
        var previousRecord: Record? = null
        var previousLevel = 0
        var line: String? = br.readLine()
        while (line != null) {
            if (line.startsWith(utf8BOM)) {
                line = line.substring(1)
            }
            val level = Integer.parseInt(line.substring(0, 1))
            var record = Record(line)
            if (level == 0) {
                val reference = record.getReferenceId()
                if (reference != null) {
                    record = when (reference.substring(0, 2)) {
                        NOTE_REFERENCE_PREFIX -> NoteRecord(record)
                        SOURCE_REFERENCE_PREFIX -> SourceRecord(record)
                        INDIVIDUAL_REFERENCE_PREFIX -> IndividualRecord(record)
                        FAMILY_REFERENCE_PREFIX -> FamilyRecord(record)
                        else -> record
                    }
                    records[reference] = record
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
    fun cleanUpReferences(
        tag: String,
        referencePrefix: String
    ) {
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

    private fun generateCanonicalReferences(
        referencePrefix: String
    ): Map<String, String> {
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

    private fun <T> MutableCollection<T>.safeAdd(element: T?) {
        if (element != null) {
            add(element)
        }
    }

    /**
     * Gets a specific individual by reference id
     */
    fun getIndividual(reference: String?): IndividualRecord? {
        if (reference in records) {
            val record = records[reference]
            if (record is IndividualRecord) {
                return record
            }
        }
        return null
    }

    /**
     * Gets a specific family by reference id
     */
    fun getFamily(reference: String?): FamilyRecord? {
        if (reference in records) {
            val record = records[reference]
            if (record is FamilyRecord) {
                return record
            }
        }
        return null
    }

    /**
     * Gets a specific source by reference id
     */
    fun getSource(reference: String?): SourceRecord? {
        if (reference in records) {
            val record = records[reference]
            if (record is SourceRecord) {
                return record
            }
        }
        return null
    }

    /**
     * Gets a specific note by reference id
     */
    fun getNote(reference: String?): NoteRecord? {
        if (reference in records) {
            val record = records[reference]
            if (record is NoteRecord) {
                return record
            }
        }
        return null
    }

    /**
     * Saves the GEDCOM records to a file
     */
    fun write(writer: Writer) {
        writer.write(utf8BOM)
        for (record: Record in records.values) {
            record.write(writer)
        }
    }
}