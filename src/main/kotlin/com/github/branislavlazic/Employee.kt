package com.github.branislavlazic

import arrow.core.Option
import arrow.data.*
import arrow.instances.nonemptylist.semigroup.semigroup
import arrow.instances.validated.applicative.applicative
import java.util.*

data class Employee(val id: Option<UUID>, val name: String, val age: Int) {

    private fun nameLengthRange(): ValidationResult<String> =
        if (name.length > 55 || name.length < 2) ValidationError(
            "name",
            "Name must have 2 to 55 characters."
        ).invalidNel() else name.valid()

    private fun ageRange(): ValidationResult<Int> =
        if (age > 120 || age < 0) ValidationError(
            "age",
            "Age must be between 0 and 120."
        ).invalidNel() else age.valid()

    fun asValidated(): ValidationResult<Employee> =
        Validated.applicative<Nel<ValidationError>>(Nel.semigroup()).map(
            nameLengthRange(),
            ageRange()
        ) {
            this
        }.fix()
}