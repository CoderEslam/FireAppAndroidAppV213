package com.devlomi.fireapp.utils

import com.devlomi.fireapp.model.ExpandableContact
import com.devlomi.fireapp.model.realms.PhoneNumber
import io.realm.RealmList

object ContactMapper {
    const val CONTACT_SEPARATOR = ",,,"

    fun mapNumbersToString(numbers: RealmList<PhoneNumber>) =
        numbers.map { it.number }.joinToString(CONTACT_SEPARATOR)

    fun mapNumbersToString(numbers: ArrayList<PhoneNumber>) =
        numbers.map { it.number }.joinToString(CONTACT_SEPARATOR)

    fun mapStringToNumbers(numbersString: String): List<PhoneNumber> {
        if (numbersString.isEmpty()) return listOf()

        val foundNumbers = numbersString.split(CONTACT_SEPARATOR)
        if (foundNumbers.isNullOrEmpty()) {
            return listOf()
        }

        return foundNumbers.map { PhoneNumber(it) }
    }

}