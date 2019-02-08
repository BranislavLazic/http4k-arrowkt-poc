package com.github.branislavlazic

import arrow.core.Some
import arrow.core.Try
import arrow.core.getOrElse
import com.fasterxml.jackson.databind.JsonNode
import org.http4k.core.*
import org.http4k.format.Jackson
import org.http4k.format.Jackson.asJsonArray
import org.http4k.format.Jackson.json
import org.http4k.lens.BodyLens
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.routes
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue


class EmployeeApi(private val employeeCache: ConcurrentLinkedQueue<Employee>) {

    private fun jsonToEmployee(node: JsonNode): Employee {
        val id = Try.invoke { node.get("id").asText() }.map { UUID.fromString(it) }.toOption()
        val name = node.get("name").asText()
        val age = node.get("age").asInt()
        return Employee(id, name, age)
    }

    private fun employeeToJson(employee: Employee): JsonNode {
        return Jackson {
            obj(
                "id" to employee.id.map { Jackson.string(it.toString()) }.getOrElse { Jackson.nullNode() },
                "name" to Jackson.string(employee.name),
                "age" to Jackson.number(employee.age)
            )
        }
    }

    private val employeeLens: BodyLens<Employee> = Body.json().map { jsonToEmployee(it) }.toLens()
    private val cacheToJsonArray = employeeCache.map { employeeToJson(it) }.asJsonArray()

    private fun createEmployeeRoute(): RoutingHttpHandler =
        "/" bind Method.POST to { req: Request ->
            tryJsonParse {
                validatedResponse(employeeLens(req).asValidated()) { employee ->
                    val generatedId = UUID.randomUUID()
                    employeeCache.add(employee.copy(id = Some(generatedId)))
                    Response(Status.CREATED)
                }
            }
        }

    private fun getEmployeesRoute(): RoutingHttpHandler =
        "/" bind Method.GET to {
            Response(Status.OK).with(Body.json().toLens() of cacheToJsonArray)
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