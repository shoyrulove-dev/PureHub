package com.purehub.app.feature.billsplitter

import kotlin.math.max

data class BillLineItem(
    val id: Int,
    val name: String,
    val amount: Double,
    val assignedPeople: Set<Int>,
)

data class PersonSplit(
    val personIndex: Int,
    val assignedTotal: Double,
    val sharedTotal: Double,
    val grandTotal: Double,
)

data class BillSplitSummary(
    val subtotalFromItems: Double,
    val remainingSharedSubtotal: Double,
    val extrasTotal: Double,
    val overallTotal: Double,
    val people: List<PersonSplit>,
) {
    val isConsistent: Boolean
        get() = overallTotal >= 0.0
}

object BillSplitterCalculator {
    fun calculate(
        totalBill: Double,
        taxAmount: Double,
        tipAmount: Double,
        peopleCount: Int,
        items: List<BillLineItem>,
    ): BillSplitSummary {
        val safePeopleCount = max(peopleCount, 1)
        val sanitizedItems = items.filter { it.amount > 0.0 }
        val itemAssignments = DoubleArray(safePeopleCount)

        sanitizedItems.forEach { item ->
            val assignees = item.assignedPeople.filter { it in 0 until safePeopleCount }
            val splitTargets = if (assignees.isEmpty()) {
                (0 until safePeopleCount).toList()
            } else {
                assignees
            }
            val perPerson = item.amount / splitTargets.size
            splitTargets.forEach { personIndex ->
                itemAssignments[personIndex] += perPerson
            }
        }

        val subtotalFromItems = sanitizedItems.sumOf { it.amount }
        val remainingSharedSubtotal = (totalBill - subtotalFromItems).coerceAtLeast(0.0)
        val extrasTotal = taxAmount + tipAmount
        val sharedPool = remainingSharedSubtotal + extrasTotal
        val sharedPerPerson = sharedPool / safePeopleCount

        val people = (0 until safePeopleCount).map { personIndex ->
            val assigned = itemAssignments[personIndex]
            val shared = sharedPerPerson
            PersonSplit(
                personIndex = personIndex,
                assignedTotal = assigned,
                sharedTotal = shared,
                grandTotal = assigned + shared,
            )
        }

        return BillSplitSummary(
            subtotalFromItems = subtotalFromItems,
            remainingSharedSubtotal = remainingSharedSubtotal,
            extrasTotal = extrasTotal,
            overallTotal = totalBill + extrasTotal,
            people = people,
        )
    }
}
