class ComparisonContext(
    val label: String,
    var leftIndividual: IndividualRecord?,
    var rightIndividual: IndividualRecord?
) {
    var leftSpouse: IndividualRecord? = null
    var rightSpouse: IndividualRecord? = null
    var leftChild: IndividualRecord? = null
    var rightChild: IndividualRecord? = null
    var leftFamily: FamilyRecord? = null
    var rightFamily: FamilyRecord? = null
    var isMale: Boolean = false

    fun withSpouse(
        leftSpouse: IndividualRecord?,
        rightSpouse: IndividualRecord?,
        isMale: Boolean
    ): ComparisonContext {
        this.leftSpouse = leftSpouse
        this.rightSpouse = rightSpouse
        this.isMale = isMale
        return this
    }

    fun withChild(
        leftChild: IndividualRecord?,
        rightChild: IndividualRecord?,
        isMale: Boolean
    ): ComparisonContext {
        this.leftChild = leftChild
        this.rightChild = rightChild
        this.isMale = isMale
        return this
    }

    fun withFamily(
        leftFamily: FamilyRecord?,
        rightFamily: FamilyRecord?
    ): ComparisonContext {
        this.leftFamily = leftFamily
        this.rightFamily = rightFamily
        return this
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ComparisonContext

        if (leftIndividual != other.leftIndividual) return false
        if (rightIndividual != other.rightIndividual) return false
        if (leftIndividual != other.leftIndividual) return false
        if (rightIndividual != other.rightIndividual) return false
        if (leftSpouse != other.leftSpouse) return false
        if (rightSpouse != other.rightSpouse) return false
        if (leftChild != other.leftChild) return false
        if (rightChild != other.rightChild) return false
        if (leftFamily != other.leftFamily) return false
        if (rightFamily != other.rightFamily) return false
        if (isMale != other.isMale) return false

        return true
    }

    override fun hashCode(): Int {
        var result = leftIndividual?.hashCode() ?: 0
        result = 31 * result + (rightIndividual?.hashCode() ?: 0)
        result = 31 * result + (leftSpouse?.hashCode() ?: 0)
        result = 31 * result + (rightSpouse?.hashCode() ?: 0)
        result = 31 * result + (leftChild?.hashCode() ?: 0)
        result = 31 * result + (rightChild?.hashCode() ?: 0)
        result = 31 * result + (leftFamily?.hashCode() ?: 0)
        result = 31 * result + (rightFamily?.hashCode() ?: 0)
        result = 31 * result + isMale.hashCode()

        return result
    }
}