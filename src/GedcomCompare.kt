import kotlin.reflect.KFunction1
import kotlin.reflect.KFunction2
import kotlin.reflect.KFunction3

class GedcomCompare(
    private val leftGedcom: Gedcom,
    private val rightGedcom: Gedcom,
    private val createFromMissing: CreateFromSide
) {
    private val idMapping: MutableMap<String, String> = mutableMapOf()
    private val comparedRecords: MutableSet<Pair<String?, String?>> = mutableSetOf()

    fun compareFrom(leftRootIndividual: String, rightRootIndividual: String) {
        val leftIndividual = leftGedcom.getIndividual(leftRootIndividual)
        val rightIndividual = rightGedcom.getIndividual(rightRootIndividual)
        if (leftIndividual == null || rightIndividual == null) {
            println("Can't find root individuals")
        } else {
            compareIndividuals(leftIndividual, rightIndividual)
        }
    }

    /**
     * Compares the same individual from both the left and the right files.
     * Name, birth, death difference or indicates if they are missing from one side.
     * Traverses to matched parents, spouses and children or indicates if they are missing from one side.
     */
    private fun compareIndividuals(
        leftIndividual: IndividualRecord,
        rightIndividual: IndividualRecord
    ) {
        val leftReferenceId = leftIndividual.getReferenceId()
        val rightReferenceId = rightIndividual.getReferenceId()
        if (!comparedRecords.contains(Pair(leftReferenceId, rightReferenceId))) {
            comparedRecords.add(Pair(leftReferenceId, rightReferenceId))
            val leftName = leftIndividual.getName()
            val rightName = rightIndividual.getName()
            println("Comparing $leftName ($leftReferenceId}) vs $rightName (${rightIndividual.getReferenceId()})")
            if (!similar(leftName, rightName)) {
                println("\tNames are significantly different: $leftName vs $rightName")
            }

            // birth & death events
            compareEvents(
                "Birth",
                leftIndividual.getBirth(),
                rightIndividual.getBirth(),
                leftIndividual,
                rightIndividual
            )
            compareEvents(
                "Death",
                leftIndividual.getDeath(),
                rightIndividual.getDeath(),
                leftIndividual,
                rightIndividual
            )

            val otherIndividuals = mutableListOf<ComparisonContext>()

            // parents
            val leftFamily = leftGedcom.getFamily(leftIndividual.getParentFamily())
            val rightFamily = rightGedcom.getFamily(rightIndividual.getParentFamily())
            otherIndividuals.add(
                ComparisonContext(
                    "Father",
                    leftGedcom.getIndividual(leftFamily?.getHusband()),
                    rightGedcom.getIndividual(rightFamily?.getHusband()),
                    null,
                    null,
                    leftIndividual,
                    rightIndividual,
                    null,
                    null
                )
            )
            otherIndividuals.add(
                ComparisonContext(
                    "Mother",
                    leftGedcom.getIndividual(leftFamily?.getWife()),
                    rightGedcom.getIndividual(rightFamily?.getWife()),
                    null,
                    null,
                    leftIndividual,
                    rightIndividual,
                    null,
                    null
                )
            )

            // marriages & children
            val spousesAndChildren = compareFamilies(
                leftIndividual.getFamilies(),
                rightIndividual.getFamilies(),
                leftIndividual,
                rightIndividual
            )
            otherIndividuals.addAll(spousesAndChildren)

            // missing on one side only
            for (otherIndividual in otherIndividuals) {
                val leftOtherIndividual = otherIndividual.leftIndividual
                val rightOtherIndividual = otherIndividual.rightIndividual
                val label = otherIndividual.label
                if (leftOtherIndividual == null || rightOtherIndividual == null) {
                    if (leftOtherIndividual != null && leftOtherIndividual != leftIndividual) {
                        println("\tFound $label on the left but not the right: ${leftOtherIndividual.getName()} (${leftOtherIndividual.getReferenceId()}})")
                        if (createFromMissing == CreateFromSide.LEFT) {
                            val newIndividual =
                                addIndividual(
                                    leftOtherIndividual,
                                    otherIndividual.rightSpouse,
                                    otherIndividual.rightChild,
                                    otherIndividual.rightFamily,
                                    leftGedcom,
                                    rightGedcom
                                )
                            println("\tAdded $label from the left to the right file (${newIndividual.getReferenceId()})")
                            otherIndividual.rightIndividual = newIndividual
                        }
                    }
                    if (rightOtherIndividual != null && rightOtherIndividual != rightIndividual) {
                        println("\tFound $label on the right but not the left: ${rightOtherIndividual.getName()} (${rightOtherIndividual.getReferenceId()})")
                        if (createFromMissing == CreateFromSide.RIGHT) {
                            val newIndividual =
                                addIndividual(
                                    rightOtherIndividual,
                                    otherIndividual.leftSpouse,
                                    otherIndividual.leftChild,
                                    otherIndividual.leftFamily,
                                    rightGedcom,
                                    leftGedcom
                                )
                            println("\tAdded $label from the right to the left file (${newIndividual.getReferenceId()})")
                            otherIndividual.leftIndividual = newIndividual
                        }
                    }
                }
            }

            // recurse into matched parents, spouses and children
            for (otherIndividual in otherIndividuals) {
                val leftOtherIndividual = otherIndividual.leftIndividual
                val rightOtherIndividual = otherIndividual.rightIndividual
                if (leftOtherIndividual != null && rightOtherIndividual != null) {
                    compareIndividuals(
                        leftOtherIndividual,
                        rightOtherIndividual
                    )
                }
            }
        }
    }

    /**
     * Matches husbands, wives and children from a marriage and compares them. Matching is lenient and uses some score calculated on the
     * marriage or individual.
     */
    private fun compareFamilies(
        leftFamilyReferences: List<String>,
        rightFamilyReferences: List<String>,
        leftIndividual: IndividualRecord,
        rightIndividual: IndividualRecord
    ): List<ComparisonContext> {
        val leftFamilies = hydrateFamilies(leftFamilyReferences, leftGedcom)
        val rightFamilies = hydrateFamilies(rightFamilyReferences, rightGedcom)
        val matchedFamilies = matchRecords(leftFamilies, rightFamilies, ::matchFamilyScore)
        val result = mutableListOf<ComparisonContext>()
        for (i in matchedFamilies.first.indices) {
            val leftFamily = matchedFamilies.first[i]
            val rightFamily = matchedFamilies.second[i]
            if (!comparedRecords.contains(Pair(leftFamily?.getReferenceId(), rightFamily?.getReferenceId()))) {
                comparedRecords.add(Pair(leftFamily?.getReferenceId(), rightFamily?.getReferenceId()))
                result.add(
                    ComparisonContext(
                        "Husband",
                        leftGedcom.getIndividual(leftFamily?.getHusband()),
                        rightGedcom.getIndividual(rightFamily?.getHusband()),
                        leftIndividual,
                        rightIndividual,
                        null,
                        null,
                        null,
                        null
                    )
                )
                result.add(
                    ComparisonContext(
                        "Wife",
                        leftGedcom.getIndividual(leftFamily?.getWife()),
                        rightGedcom.getIndividual(rightFamily?.getWife()),
                        leftIndividual,
                        rightIndividual,
                        null,
                        null,
                        null,
                        null
                    )
                )

                compareEvents(
                    "Marriage",
                    leftFamily?.getMarriage(),
                    rightFamily?.getMarriage(),
                    leftFamily,
                    rightFamily
                )

                // children
                if (leftFamily != null && rightFamily != null) {
                    val leftChildren = hydrateIndividuals(leftFamily.getChildren(), leftGedcom)
                    val rightChildren = hydrateIndividuals(rightFamily.getChildren(), rightGedcom)
                    val matchedChildren = matchRecords(leftChildren, rightChildren, ::matchIndividualScore)
                    for (j in matchedChildren.first.indices) {
                        result.add(
                            ComparisonContext(
                                "Child",
                                matchedChildren.first[j],
                                matchedChildren.second[j],
                                null,
                                null,
                                null,
                                null,
                                leftFamily,
                                rightFamily
                            )
                        )
                    }
                }
            }
        }
        return result
    }

    /**
     * Matches all the specified records on the left (for example families or children) with all corresponding records
     * on the right, ranks them by score decreasing and picks the highest score matches. What isn't matched remains
     * as entries on one side only
     */
    private fun <T> matchRecords(
        leftRecords: List<T>,
        rightRecords: List<T>,
        matchScoreMethod: (T, T) -> Int
    ): Pair<List<T?>, List<T?>> {
        val left = mutableListOf<T?>()
        val right = mutableListOf<T?>()
        val pairWiseMatches = mutableListOf<Triple<T, T, Int>>()
        for (record in leftRecords) {
            pairWiseMatches.addAll(pairAndScoreRecord(record, rightRecords, matchScoreMethod))
        }
        val sortedPairWiseMatches = pairWiseMatches.sortedWith(Comparator.comparingInt { -it.third })
        val placedRecords = mutableSetOf<T>()
        for (triple in sortedPairWiseMatches) {
            if (triple.first !in placedRecords && triple.second !in placedRecords) {
                placedRecords.add(triple.first)
                left.add(triple.first)
                if (triple.third > 0) {
                    placedRecords.add(triple.second)
                    right.add(triple.second)
                } else {
                    right.add(null)
                }
            }
        }
        // unmatched records on the left
        for (record in leftRecords) {
            if (record !in placedRecords) {
                left.add(record)
                right.add(null)
            }
        }
        // unmatched records on the right
        for (record in rightRecords) {
            if (record !in placedRecords) {
                left.add(null)
                right.add(record)
            }
        }
        return Pair(left, right)
    }

    /**
     * Pairs a specific record from the left file with a list of records from the right file and scores them
     */
    private fun <T> pairAndScoreRecord(
        leftRecord: T,
        rightRecords: List<T>,
        matchScoreMethod: (T, T) -> Int
    ): List<Triple<T, T, Int>> {
        val result = mutableListOf<Triple<T, T, Int>>()
        for (rightRecord in rightRecords) {
            result.add(
                Triple(
                    leftRecord,
                    rightRecord,
                    matchScoreMethod.invoke(leftRecord, rightRecord)
                )
            )
        }
        return result
    }

    /**
     * Try to score a match between families. Marriage event is the most important, followed by similarity in spouse names
     */
    private fun matchFamilyScore(
        leftFamily: FamilyRecord,
        rightFamily: FamilyRecord
    ): Int {
        var result = 0
        if (leftFamily.getMarriage()?.matches(rightFamily.getMarriage()) == true) {
            result += 20
        }
        if (similar(
                leftGedcom.getIndividual(leftFamily.getHusband())?.getName(),
                rightGedcom.getIndividual(rightFamily.getHusband())?.getName()
            )
        ) {
            result += 5
        }
        if (similar(
                leftGedcom.getIndividual(leftFamily.getWife())?.getName(),
                rightGedcom.getIndividual(rightFamily.getWife())?.getName()
            )
        ) {
            result += 4
        }
        return result
    }

    /**
     * Try to score a match between individuals. Birth is the most important, followed by death and name. Ensure same gender
     */
    private fun matchIndividualScore(leftIndividual: IndividualRecord, rightIndividual: IndividualRecord): Int {
        var result = 0
        if (leftIndividual.getGender() != null && rightIndividual.getGender() != null && leftIndividual.getGender() != rightIndividual.getGender()) {
            return result
        }
        if (leftIndividual.getBirth()?.matches(rightIndividual.getBirth()) == true) {
            result += 20
        }
        if (leftIndividual.getDeath()?.matches(rightIndividual.getDeath()) == true) {
            result += 10
        }
        if (similar(leftIndividual.getName(), rightIndividual.getName())) {
            result += 5
        }
        return result
    }

    private fun hydrateFamilies(familyReferences: List<String>, gedcom: Gedcom): List<FamilyRecord> {
        val families = mutableListOf<FamilyRecord>()
        for (reference in familyReferences) {
            families.safeAdd(gedcom.getFamily(reference))
        }
        return families
    }

    private fun hydrateIndividuals(individualReferences: List<String>, gedcom: Gedcom): List<IndividualRecord> {
        val individuals = mutableListOf<IndividualRecord>()
        for (reference in individualReferences) {
            individuals.safeAdd(gedcom.getIndividual(reference))
        }
        return individuals
    }

    private fun compareEvents(
        label: String,
        event1: Event?,
        event2: Event?,
        leftRecord: Record?,
        rightRecord: Record?
    ) {
        var leftEvent = event1
        if (event1?.date == null && event1?.place == null) {
            leftEvent = null
        }
        var rightEvent = event2
        if (event2?.date == null && event2?.place == null) {
            rightEvent = null
        }
        if (leftEvent == null || rightEvent == null) {
            if (leftEvent != null) {
                println("\tFound $label on the left but not the right: $leftEvent")
                if (createFromMissing == CreateFromSide.LEFT) {
                    addEvent(leftEvent, rightRecord, leftGedcom, rightGedcom)
                    println("\tAdded $label from the left to the right file")
                }
            }
            if (rightEvent != null) {
                println("\tFound $label on the right but not the left: $rightEvent")
                if (createFromMissing == CreateFromSide.RIGHT) {
                    addEvent(rightEvent, leftRecord, rightGedcom, leftGedcom)
                    println("\tAdded $label from the right to the left file")
                }
            }
        } else {
            if (!leftEvent.matches(rightEvent)) {
                println("\t$label different: $leftEvent vs $rightEvent")
            }
        }
    }


    private fun addIndividual(
        individual: IndividualRecord,
        spouse: IndividualRecord?,
        child: IndividualRecord?,
        family: FamilyRecord?,
        fromGedcom: Gedcom,
        toGedcom: Gedcom
    ): IndividualRecord {
        val referenceId = toGedcom.generateIndividualReferenceId()
        val clone = individual.clone()
        clone.setReferenceId(referenceId)
        toGedcom.addIndividual(referenceId, clone)
        // copy Notes and Sources
        copyReferences(
            NOTE_TAG,
            clone,
            Gedcom::generateNoteReferenceId,
            Gedcom::getNote,
            Gedcom::addNote,
            NoteRecord::clone,
            fromGedcom,
            toGedcom
        )
        copyReferences(
            SOURCE_TAG,
            clone,
            Gedcom::generateSourceReferenceId,
            Gedcom::getSource,
            Gedcom::addSource,
            SourceRecord::clone,
            fromGedcom,
            toGedcom
        )
        // clear families
        clone.removeParentFamily()
        clone.removeFamilies()
        // add spouse
        if (spouse != null) {
            val familyReferenceId = toGedcom.generateFamilyReferenceId()
            val familyClone = newFamily(familyReferenceId)
            toGedcom.addFamily(familyReferenceId, familyClone)
            var husband: IndividualRecord? = clone
            var wife: IndividualRecord? = spouse
            if ((husband?.getGender() != null && husband.getGender() == 'F') || (wife?.getGender() != null && wife.getGender() == 'M')) {
                husband = spouse
                wife = clone
            }
            var rId = husband?.getReferenceId()
            if (rId != null) {
                familyClone.setHusband(rId)
            }
            rId = wife?.getReferenceId()
            if (rId != null) {
                familyClone.setWife(rId)
            }
            husband?.addFamily(familyClone)
            wife?.addFamily(familyClone)
        }
        // add child
        if (family != null) {
            family.addChild(referenceId)
            clone.addParentFamily(family)
        }
        // add parent
        if (child != null) {
            var parentFamily = toGedcom.getFamily(child.getParentFamily())
            if (parentFamily == null) {
                val parentReferenceId = toGedcom.generateFamilyReferenceId()
                parentFamily = newFamily(parentReferenceId)
                toGedcom.addFamily(parentReferenceId, parentFamily)
                child.addParentFamily(parentFamily)
                val childReferenceId = child.getReferenceId()
                if (childReferenceId != null) {
                    parentFamily.addChild(childReferenceId)
                }
            }
            if (parentFamily.getHusband() == null || clone.getGender() == 'M') {
                parentFamily.setHusband(referenceId)
            } else {
                parentFamily.setWife(referenceId)
            }
            clone.addFamily(parentFamily)
        }

        return clone
    }

    private fun <T : Record> copyReferences(
        tag: String,
        record: Record,
        generateReferenceId: KFunction1<Gedcom, String>,
        getRecord: KFunction2<Gedcom, String?, T?>,
        addRecord: KFunction3<Gedcom, String, T, Unit>,
        cloneRecord: KFunction1<T, T>,
        fromGedcom: Gedcom,
        toGedcom: Gedcom
    ) {
        if (record.text.contains(tag)) {
            val id = record.getReference()
            if (id != null) {
                val newId = idMapping.getOrPut(id, { generateReferenceId.invoke(toGedcom) })
                record.text = record.text.substring(0, 7) + newId
                val referenceRecord = getRecord.invoke(fromGedcom, id)
                if (referenceRecord != null) {
                    val clone = cloneRecord.invoke(referenceRecord)
                    clone.setReferenceId(newId)
                    addRecord.invoke(toGedcom, newId, clone)
                }
            }
        }
        for (subRecord in record.subRecords) {
            copyReferences(
                tag,
                subRecord,
                generateReferenceId,
                getRecord,
                addRecord,
                cloneRecord,
                fromGedcom,
                toGedcom
            )
        }
    }

    private fun addEvent(
        event: Event,
        record: Record?,
        fromGedcom: Gedcom,
        toGedcom: Gedcom
    ) {
        val clone = event.record.clone()
        record?.addSubRecord(clone)
        // copy Notes and Sources
        copyReferences(
            NOTE_TAG,
            clone,
            Gedcom::generateNoteReferenceId,
            Gedcom::getNote,
            Gedcom::addNote,
            NoteRecord::clone,
            fromGedcom,
            toGedcom
        )
        copyReferences(
            SOURCE_TAG,
            clone,
            Gedcom::generateSourceReferenceId,
            Gedcom::getSource,
            Gedcom::addSource,
            SourceRecord::clone,
            fromGedcom,
            toGedcom
        )
    }

    private data class ComparisonContext(
        val label: String,
        var leftIndividual: IndividualRecord?,
        var rightIndividual: IndividualRecord?,
        var leftSpouse: IndividualRecord?,
        var rightSpouse: IndividualRecord?,
        var leftChild: IndividualRecord?,
        var rightChild: IndividualRecord?,
        var leftFamily: FamilyRecord?,
        var rightFamily: FamilyRecord?
    )
}
