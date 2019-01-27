package com.github.branislavlazic

import arrow.core.Try
import arrow.core.getOrElse
import arrow.data.Nel
import arrow.data.Validated
import arrow.data.valueOr
import org.http4k.core.Body
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.with
import org.http4k.format.Jackson.auto
import org.http4k.server.SunHttp
import org.http4k.server.asServer
import java.util.concurrent.ConcurrentLinkedQueue

data class ValidationError(val code: String, val message: String)

fun <E> validatedResponse(validated: Validated<Nel<ValidationError>, E>, successHandler: (E) -> Response): Response =
    validated.map { successHandler(it) }
        .valueOr { errors -> Response(Status.UNPROCESSABLE_ENTITY).with(Body.auto<List<ValidationError>>().toLens() of errors.all) }

fun tryJsonParse(lensHandler: () -> Response): Response =
    Try.invoke { lensHandler() }.getOrElse { Response(Status.BAD_REQUEST).body("Failed to unmarshal an entity.") }

fun main() {
    val employeeCache = ConcurrentLinkedQueue<Employee>()
    EmployeeApi(employeeCache).employeeRoutes().asServer(SunHttp(8000)).start()
}