package com.github.branislavlazic

import org.http4k.core.*
import org.http4k.format.Jackson.auto
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.routes
import java.util.concurrent.ConcurrentLinkedQueue

class EmployeeApi(private val employeeCache: ConcurrentLinkedQueue<Employee>) {
    private val employeeLens = Body.auto<Employee>().toLens()
    private val employeeListLens = Body.auto<List<Employee>>().toLens()

    private fun createEmployeeRoute(): RoutingHttpHandler =
        "/" bind Method.POST to { req ->
            tryJsonParse {
                validatedResponse(employeeLens(req).asValidated()) {
                    employeeCache.add(it)
                    Response(Status.CREATED)
                }
            }
        }

    private fun getEmployeesRoute(): RoutingHttpHandler =
        "/" bind Method.GET to {
            Response(Status.OK).with(employeeListLens of employeeCache.toList())
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