import java.util.*
import kotlin.Comparator
import kotlin.reflect.KFunction1
import kotlin.reflect.KFunction2
import kotlin.reflect.KFunction3

class GedcomCompare(
    private val leftGedcom: Gedcom,
    private val rightGedcom: Gedcom,
    private val createFromMissing: CreateFromSide
) {
    private val idMapping: MutableMap<String, String> = mutableMapOf()
    private val comparedRecords: MutableSet<ComparisonContext> = mutableSetOf()

    fun compareFrom(leftRootIndividual: String, rightRootIndividual: String) {
        val leftIndividual = leftGedcom.getIndividual(leftRootIndividual)
        val rightIndividual = rightGedcom.getIndividual(rightRootIndividual)
        if (leftIndividual == null || rightIndividual == null) {
            println("Can't find root individuals")
        } else {
            val comparisonContext = ComparisonContext("", leftIndividual, rightIndividual)
            if (createFromMissing != CreateFromSide.NONE) {
                println("Mapping all matched individuals")
                compareIndividuals(comparisonContext, true)
                println("Done mapping all matched individuals")
                comparedRecords.clear()
            }
            compareIndividuals(comparisonContext, false)
        }
    }

    /**
     * Compares the same individual from both the left and the right files.
     * Traverses to matched parents, spouses and children
     */
    private fun compareIndividuals(
        initialContext: ComparisonContext,
        mapOnly: Boolean
    ) {
        val stack: Stack<ComparisonContext> = Stack()
        stack.push(initialContext)

        while (!stack.isEmpty()) {
            val comparisonContext = stack.pop()
            val leftIndividual = comparisonContext.leftIndividual
            val rightIndividual = comparisonContext.rightIndividual
            if (leftIndividual != null && rightIndividual != null) {
                if (!comparedRecords.contains(comparisonContext)) {
                    comparedRecords.add(comparisonContext)

                    val leftLastname = leftIndividual.getLastname()
                    val rightLastname = rightIndividual.getLastname()
                    if (leftLastname != null && rightLastname != null && !isSimilar(leftLastname, rightLastname)) {
                        // something isn't right if the lastnames are significantly different, so stop traversing
                        // to their spouses, parents and children
                        if (!mapOnly) {
                            println("Individuals with significantly different lastnames: $leftLastname vs $rightLastname. Can't determine which side is valid.")
                        }
                    } else {
                        val otherIndividuals = compareIndividuals(leftIndividual, rightIndividual, mapOnly)

                        // recurse into matched parents, spouses and children
                        for (otherIndividual in otherIndividuals) {
                            if (otherIndividual.leftIndividual != null && otherIndividual.rightIndividual != null) {
                                stack.push(otherIndividual)
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Compares the same individual from both the left and the right files.
     * Name, birth, death difference or indicates if they are missing from one side.
     * Returns matched parents, spouses and children or indicates if they are missing from one side.
     */
    private fun compareIndividuals(
        leftIndividual: IndividualRecord,
        rightIndividual: IndividualRecord,
        mapOnly: Boolean
    ): MutableList<ComparisonContext> {
        val leftReferenceId = leftIndividual.getReferenceId()
        val rightReferenceId = rightIndividual.getReferenceId()
        if (mapOnly && leftReferenceId != null && rightReferenceId != null) {
            if (createFromMissing == CreateFromSide.LEFT) {
                idMapping[leftReferenceId] = rightReferenceId
            } else if (createFromMissing == CreateFromSide.RIGHT) {
                idMapping[rightReferenceId] = leftReferenceId
            }
        }

        if (!mapOnly) {
            val leftName = leftIndividual.getName()
            val rightName = rightIndividual.getName()
            println("Comparing $leftName ($leftReferenceId}) vs $rightName (${rightIndividual.getReferenceId()})")
            if (!isSimilar(leftName, rightName)) {
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
        }

        val otherIndividuals = mutableListOf<ComparisonContext>()

        // parents
        val leftFamily = leftGedcom.getFamily(leftIndividual.getParentFamily())
        val rightFamily = rightGedcom.getFamily(rightIndividual.getParentFamily())
        otherIndividuals.add(
            ComparisonContext(
                "Father",
                leftGedcom.getIndividual(leftFamily?.getHusband()),
                rightGedcom.getIndividual(rightFamily?.getHusband())
            ).withChild(leftIndividual, rightIndividual, true)
        )
        otherIndividuals.add(
            ComparisonContext(
                "Mother",
                leftGedcom.getIndividual(leftFamily?.getWife()),
                rightGedcom.getIndividual(rightFamily?.getWife())
            ).withChild(leftIndividual, rightIndividual, false)
        )

        // marriages & children
        val spousesAndChildren = compareFamilies(
            leftIndividual.getFamilies(),
            rightIndividual.getFamilies(),
            leftIndividual,
            rightIndividual,
            mapOnly
        )
        otherIndividuals.addAll(spousesAndChildren)

        // missing on one side only
        if (!mapOnly) {
            for (otherIndividual in otherIndividuals) {
                val leftOtherIndividual = otherIndividual.leftIndividual
                val rightOtherIndividual = otherIndividual.rightIndividual
                val label = otherIndividual.label
                if (leftOtherIndividual == null || rightOtherIndividual == null) {
                    if (leftOtherIndividual != null && leftOtherIndividual != leftIndividual) {
                        println("\tFound $label on the left but not the right: ${leftOtherIndividual.getName()} (${leftOtherIndividual.getReferenceId()}})")
                        if (createFromMissing == CreateFromSide.LEFT) {
                            val newIndividual =
                                copyIndividual(
                                    leftOtherIndividual,
                                    otherIndividual.rightSpouse,
                                    otherIndividual.rightChild,
                                    otherIndividual.rightFamily,
                                    otherIndividual.isMale,
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
                                copyIndividual(
                                    rightOtherIndividual,
                                    otherIndividual.leftSpouse,
                                    otherIndividual.leftChild,
                                    otherIndividual.leftFamily,
                                    otherIndividual.isMale,
                                    rightGedcom,
                                    leftGedcom
                                )
                            println("\tAdded $label from the right to the left file (${newIndividual.getReferenceId()})")
                            otherIndividual.leftIndividual = newIndividual
                        }
                    }
                }
            }
        }
        return otherIndividuals
    }

    /**
     * Matches husbands, wives and children from a marriage and compares them. Matching is lenient and uses some score calculated on the
     * marriage or individual.
     */
    private fun compareFamilies(
        leftFamilyReferences: List<String>,
        rightFamilyReferences: List<String>,
        leftIndividual: IndividualRecord,
        rightIndividual: IndividualRecord,
        mapOnly: Boolean
    ): List<ComparisonContext> {
        val leftFamilies = hydrateFamilies(leftFamilyReferences, leftGedcom)
        val rightFamilies = hydrateFamilies(rightFamilyReferences, rightGedcom)
        val matchedFamilies = matchRecords(leftFamilies, rightFamilies, ::matchFamilyScore)
        val result = mutableListOf<ComparisonContext>()
        for (i in matchedFamilies.first.indices) {
            val leftFamily = matchedFamilies.first[i]
            val rightFamily = matchedFamilies.second[i]
            result.add(
                ComparisonContext(
                    "Husband",
                    leftGedcom.getIndividual(leftFamily?.getHusband()),
                    rightGedcom.getIndividual(rightFamily?.getHusband())
                ).withSpouse(leftIndividual, rightIndividual, true)
            )
            result.add(
                ComparisonContext(
                    "Wife",
                    leftGedcom.getIndividual(leftFamily?.getWife()),
                    rightGedcom.getIndividual(rightFamily?.getWife())
                ).withSpouse(leftIndividual, rightIndividual, false)
            )

            if (!mapOnly) {
                compareEvents(
                    "Marriage",
                    leftFamily?.getMarriage(),
                    rightFamily?.getMarriage(),
                    leftFamily,
                    rightFamily
                )
            }

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
                            matchedChildren.second[j]
                        ).withFamily(leftFamily, rightFamily)
                    )
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
            result += 40
        }
        var leftHusband = leftGedcom.getIndividual(leftFamily.getHusband())
        var rightHusband = rightGedcom.getIndividual(rightFamily.getHusband())
        if (leftHusband != null && rightHusband != null) {
            result += matchIndividualScore(leftHusband, rightHusband)
        }
        var leftWife = leftGedcom.getIndividual(leftFamily.getWife())
        var rightWife = rightGedcom.getIndividual(rightFamily.getWife())
        if (leftWife != null && rightWife != null) {
            result += matchIndividualScore(leftWife, rightWife)
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
        val leftName = leftIndividual.getName()
        val rightName = rightIndividual.getName()
        if (leftName != null && rightName != null && isSimilar(leftName, rightName)) {
            result += 5 - distance(leftName, rightName)
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


    /**
     * Copies an individual record from one gedcom to another gedcom file, along with family records and/or
     * parent/child relationship as needed
     */
    private fun copyIndividual(
        individual: IndividualRecord,
        spouse: IndividualRecord?,
        child: IndividualRecord?,
        family: FamilyRecord?,
        isMale: Boolean,
        fromGedcom: Gedcom,
        toGedcom: Gedcom
    ): IndividualRecord {
        val individualReferenceId = individual.getReferenceId()
        var clone = toGedcom.getIndividual(idMapping[individualReferenceId])
        if (clone == null) {
            clone = individual.clone()
            val newId = toGedcom.generateIndividualReferenceId()
            clone.setReferenceId(newId)
            toGedcom.addIndividual(newId, clone)
            if (individualReferenceId != null) {
                idMapping[individualReferenceId] = newId
            }
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
        }
        val referenceId = clone.getReferenceId()!!
        // add spouse
        if (spouse != null) {
            val familyReferenceId = toGedcom.generateFamilyReferenceId()
            val familyClone = newFamily(familyReferenceId)
            toGedcom.addFamily(familyReferenceId, familyClone)
            var husband: IndividualRecord? = clone
            var wife: IndividualRecord? = spouse
            if (!isMale) {
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
            if (isMale) {
                parentFamily.setHusband(referenceId)
            } else {
                parentFamily.setWife(referenceId)
            }
            clone.addFamily(parentFamily)
        }

        return clone
    }

    /**
     * Compares 2 specific events
     */
    private fun compareEvents(
        label: String,
        leftEvent: Event?,
        rightEvent: Event?,
        leftRecord: Record?,
        rightRecord: Record?
    ) {
        if (leftEvent == null || rightEvent == null) {
            if (leftEvent != null) {
                println("\tFound $label on the left but not the right: $leftEvent")
                if (createFromMissing == CreateFromSide.LEFT) {
                    if (copyEvent(leftEvent, rightRecord, leftGedcom, rightGedcom)) {
                        println("\tAdded $label from the left to the right file")
                    }
                }
            }
            if (rightEvent != null) {
                println("\tFound $label on the right but not the left: $rightEvent")
                if (createFromMissing == CreateFromSide.RIGHT) {
                    if (copyEvent(rightEvent, leftRecord, rightGedcom, leftGedcom)) {
                        println("\tAdded $label from the right to the left file")
                    }
                }
            }
        } else {
            if (!leftEvent.matches(rightEvent)) {
                println("\t$label different: $leftEvent vs $rightEvent")
            }
        }
    }

    /**
     * Copies note and source references from one gedcom to another gedcom file
     */
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

    /**
     * Copies an event and its associated notes and sources from one gedcom to another gedcom file
     */
    private fun copyEvent(
        event: Event,
        record: Record?,
        fromGedcom: Gedcom,
        toGedcom: Gedcom
    ): Boolean {
        if (record != null) {
            val clone = event.record.clone()
            record.addSubRecord(clone)
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
            return true
        }
        return false
    }

}
