package com.github.branislavlazic

import arrow.core.Try
import arrow.core.getOrElse
import arrow.data.*
import arrow.instances.nonemptylist.semigroup.semigroup
import arrow.instances.validated.applicative.applicative
import org.http4k.core.*
import org.http4k.core.Method.POST
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.CREATED
import org.http4k.core.Status.Companion.OK
import org.http4k.format.Jackson.auto
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.SunHttp
import org.http4k.server.asServer
import java.util.concurrent.ConcurrentLinkedQueue

data class ValidationError(val code: String, val message: String)

fun <E> validatedResponse(validated: Validated<Nel<ValidationError>, E>, successHandler: (E) -> Response): Response =
    validated.map { successHandler(it) }
        .valueOr { errors -> Response(Status.UNPROCESSABLE_ENTITY).with(Body.auto<List<ValidationError>>().toLens() of errors.all) }

fun tryJsonParse(lensHandler: () -> Response): Response =
    Try.invoke { lensHandler() }.getOrElse { Response(BAD_REQUEST).body("Failed to unmarshal an entity.") }

// Employee entity with validation constraints
data class Employee(val id: Int?, val name: String, val age: Int) {

    private fun nameLengthRange(): Validated<Nel<ValidationError>, String> =
        if (name.length > 55) ValidationError(
            "name",
            "Name cannot have more than 55 characters."
        ).invalidNel() else name.valid()

    private fun ageRange(): Validated<Nel<ValidationError>, Int> =
        if (age > 120 || age < 0) ValidationError(
            "age",
            "Age must be between 0 and 120."
        ).invalidNel() else age.valid()

    fun asValidated(): Validated<Nel<ValidationError>, Employee> =
        Validated.applicative<Nel<ValidationError>>(Nel.semigroup()).map(
            nameLengthRange(),
            ageRange()
        ) {
            this
        }.fix()
}

class EmployeeApi(private val employeeCache: ConcurrentLinkedQueue<Employee>) {
    private val employeeLens = Body.auto<Employee>().toLens()
    private val employeeListLens = Body.auto<List<Employee>>().toLens()

    private fun createEmployeeRoute(): RoutingHttpHandler =
        "/" bind POST to { req ->
            tryJsonParse {
                validatedResponse(employeeLens(req).asValidated()) {
                    employeeCache.add(it)
                    Response(CREATED)
                }
            }
        }

    private fun getEmployeesRoute(): RoutingHttpHandler =
        "/" bind Method.GET to {
            Response(OK).with(employeeListLens of employeeCache.toList())
        }

    fun employeeRoutes(): RoutingHttpHandler {
        return routes(
            "/employees" bind routes(
                createEmployeeRoute(),
                getEmployeesRoute()
            )
        )
    }
}

fun main(args: Array<String>) {
    val employeeCache = ConcurrentLinkedQueue<Employee>()
    EmployeeApi(employeeCache).employeeRoutes().asServer(SunHttp(8000)).start()
}