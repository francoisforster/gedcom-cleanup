import kotlin.math.min

/**
 * https://gist.github.com/ademar111190/34d3de41308389a0d0d8
 */
fun levenshtein(lhs: CharSequence, rhs: CharSequence): Int {
    val lhsLength = lhs.length
    val rhsLength = rhs.length

    var cost = Array(lhsLength) { it }
    var newCost = Array(lhsLength) { 0 }

    for (i in 1 until rhsLength) {
        newCost[0] = i

        for (j in 1 until lhsLength) {
            val match = if (lhs[j - 1] == rhs[i - 1]) 0 else 1

            val costReplace = cost[j - 1] + match
            val costInsert = cost[j] + 1
            val costDelete = newCost[j - 1] + 1

            newCost[j] = min(min(costInsert, costDelete), costReplace)
        }

        val swap = cost
        cost = newCost
        newCost = swap
    }

    return cost[lhsLength - 1]
}

fun isSimilar(left: String?, right: String?): Boolean {
    if (left == null || right == null) {
        return false
    }
    return distance(left, right) <= Math.max(1, (left.length + right.length) / 6)
}

fun distance(left: String, right: String): Int {
    return levenshtein(left, right)
}
