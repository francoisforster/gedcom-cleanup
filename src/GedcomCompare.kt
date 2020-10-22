class GedcomCompare(private val leftGedcom: Gedcom, private val rightGedcom: Gedcom) {
    fun compareFrom(leftRootIndividual: String, rightRootIndividual: String) {
        val leftIndividual = leftGedcom.getIndividual(leftRootIndividual)
        val rightIndividual = rightGedcom.getIndividual(rightRootIndividual)
        if (leftIndividual == null || rightIndividual == null) {
            println("Can't find root individuals")
        } else {
            val comparedIndividuals = mutableSetOf<String>()
            compareIndividuals(leftIndividual, rightIndividual, comparedIndividuals)
        }
    }

    private fun compareIndividuals(
        leftIndividual: IndividualRecord,
        rightIndividual: IndividualRecord,
        comparedIndividuals: MutableSet<String>
    ) {
        val leftReferenceId = leftIndividual.getReferenceId()
        if (!comparedIndividuals.contains(leftReferenceId)) {
            comparedIndividuals.safeAdd(leftReferenceId)
            val leftName = leftIndividual.getName()
            val rightName = rightIndividual.getName()
            println("Comparing $leftName ($leftReferenceId}) vs $rightName (${rightIndividual.getReferenceId()})")
            if (!similar(leftName, rightName)) {
                println("\tNames are significantly different: $leftName vs $rightName")
            }

            // birth & death
            compareEvents("Birth", leftIndividual.getBirth(), rightIndividual.getBirth())
            compareEvents("Death", leftIndividual.getDeath(), rightIndividual.getDeath())

            val otherIndividuals = mutableListOf<Triple<IndividualRecord?, IndividualRecord?, String>>()

            // parents
            val leftFamily = leftGedcom.getFamily(leftIndividual.getParentFamily())
            val rightFamily = rightGedcom.getFamily(rightIndividual.getParentFamily())
            otherIndividuals.add(
                Triple(
                    leftGedcom.getIndividual(leftFamily?.getHusband()),
                    rightGedcom.getIndividual(rightFamily?.getHusband()),
                    "Father"
                )
            )
            otherIndividuals.add(
                Triple(
                    leftGedcom.getIndividual(leftFamily?.getWife()),
                    rightGedcom.getIndividual(rightFamily?.getWife()),
                    "Mother"
                )
            )

            // marriages & children
            val spousesAndChildren =
                compareFamilies(leftIndividual.getFamilies(), rightIndividual.getFamilies())
            otherIndividuals.addAll(spousesAndChildren)

            // missing on one side only
            for (otherIndividual in otherIndividuals) {
                val leftOtherIndividual = otherIndividual.first
                val rightOtherIndividual = otherIndividual.second
                val label = otherIndividual.third
                if (leftOtherIndividual == null || rightOtherIndividual == null) {
                    if (leftOtherIndividual != null && leftOtherIndividual != leftIndividual) {
                        println("\tFound $label on the left but not the right: ${leftOtherIndividual.getName()} (${leftOtherIndividual.getReferenceId()}})")
                    }
                    if (rightOtherIndividual != null && rightOtherIndividual != rightIndividual) {
                        println("\tFound $label on the right but not the left: ${rightOtherIndividual.getName()} (${rightOtherIndividual.getReferenceId()})")
                    }
                }
            }

            // recurse into matched parents, spouses and children
            for (otherIndividual in otherIndividuals) {
                val leftOtherIndividual = otherIndividual.first
                val rightOtherIndividual = otherIndividual.second
                if (leftOtherIndividual != null && rightOtherIndividual != null) {
                    compareIndividuals(
                        leftOtherIndividual,
                        rightOtherIndividual,
                        comparedIndividuals
                    )
                }
            }
        }
    }

    private fun compareFamilies(
        leftFamilyReferences: List<String>,
        rightFamilyReferences: List<String>
    ): List<Triple<IndividualRecord?, IndividualRecord?, String>> {
        val leftFamilies = hydrateFamilies(leftFamilyReferences, leftGedcom)
        val rightFamilies = hydrateFamilies(rightFamilyReferences, rightGedcom)
        val matchedFamilies = matchRecords(leftFamilies, rightFamilies, ::matchFamily)
        val result = mutableListOf<Triple<IndividualRecord?, IndividualRecord?, String>>()
        for (i in matchedFamilies.first.indices) {
            val leftFamily = matchedFamilies.first[i]
            val rightFamily = matchedFamilies.second[i]
            result.add(
                Triple(
                    leftGedcom.getIndividual(leftFamily?.getHusband()),
                    rightGedcom.getIndividual(rightFamily?.getHusband()),
                    "Husband"
                )
            )
            result.add(
                Triple(
                    leftGedcom.getIndividual(leftFamily?.getWife()),
                    rightGedcom.getIndividual(rightFamily?.getWife()),
                    "Wife"
                )
            )

            compareEvents("Marriage", leftFamily?.getMarriage(), rightFamily?.getMarriage())

            // children
            if (leftFamily != null && rightFamily != null) {
                val leftChildren = hydrateIndividuals(leftFamily.getChildren(), leftGedcom)
                val rightChildren = hydrateIndividuals(rightFamily.getChildren(), rightGedcom)
                val matchedChildren = matchRecords(leftChildren, rightChildren, ::matchChild)
                for (j in matchedChildren.first.indices) {
                    result.add(
                        Triple(
                            matchedChildren.first[j],
                            matchedChildren.second[j],
                            "Child"
                        )
                    )
                }
            }
        }
        return result
    }

    private fun <T> matchRecords(
        leftRecords: List<T>,
        rightRecords: List<T>,
        matchScoreMethod: (T, List<T>) -> List<Triple<T, T, Int>>
    ): Pair<List<T?>, List<T?>> {
        val left = mutableListOf<T?>()
        val right = mutableListOf<T?>()
        val pairWiseMatches = mutableListOf<Triple<T, T, Int>>()
        for (record in leftRecords) {
            pairWiseMatches.addAll(matchScoreMethod.invoke(record, rightRecords))
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
        for (record in leftRecords) {
            if (record !in placedRecords) {
                left.add(record)
                right.add(null)
            }
        }
        for (record in rightRecords) {
            if (record !in placedRecords) {
                left.add(null)
                right.add(record)
            }
        }
        return Pair(left, right)
    }

    private fun matchFamily(
        leftFamily: FamilyRecord,
        rightFamilies: List<FamilyRecord>
    ): List<Triple<FamilyRecord, FamilyRecord, Int>> {
        val result = mutableListOf<Triple<FamilyRecord, FamilyRecord, Int>>()
        for (rightFamily in rightFamilies) {
            result.add(
                Triple(
                    leftFamily,
                    rightFamily,
                    matchFamilyScore(leftFamily, rightFamily)
                )
            )
        }
        return result
    }

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

    private fun matchChild(
        leftChild: IndividualRecord,
        rightChildren: List<IndividualRecord>
    ): List<Triple<IndividualRecord, IndividualRecord, Int>> {
        val result = mutableListOf<Triple<IndividualRecord, IndividualRecord, Int>>()
        for (rightChild in rightChildren) {
            result.add(
                Triple(
                    leftChild,
                    rightChild,
                    matchIndividualScore(leftChild, rightChild)
                )
            )
        }
        return result
    }

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

    private fun compareEvents(label: String, event1: Event?, event2: Event?) {
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
                println("\tFound $label on left but not right: $leftEvent")
            }
            if (rightEvent != null) {
                println("\tFound $label on right but not left: $rightEvent")
            }
        } else {
            if (!leftEvent.matches(rightEvent)) {
                println("\t$label different: $leftEvent vs $rightEvent")
            }
        }
    }
}
