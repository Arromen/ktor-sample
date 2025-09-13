package com.example

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.jackson.*
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.plugins.swagger.*
import io.ktor.util.pipeline.*
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.util.AttributeKey

data class Item(val id: Int, val name: String)

val itemsList = mutableListOf(Item(1, "Phone"), Item(2, "Laptop"))

fun main(args: Array<String>): Unit = EngineMain.main(args)

fun Application.module() {
    val log = LoggerFactory.getLogger("CustomMiddleware")

    if (pluginOrNull(CallLogging) == null) {
        install(CallLogging) {
            level = Level.INFO
        }
    }

    intercept(ApplicationCallPipeline.Plugins) {
        if (call.request.httpMethod == HttpMethod.Post || call.request.httpMethod == HttpMethod.Put) {
            val bodyText = call.receiveText()
            log.info("Request body: $bodyText")
            call.attributes.put(BodyAttributeKey, bodyText)
            proceed()
        } else {
            proceed()
        }
    }

    install(ContentNegotiation) {
        jackson()
    }

    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf("error" to (cause.message ?: "Internal server error"))
            )
        }
    }

    routing {
        swaggerUI(path = "swagger", swaggerFile = "openapi/documentation.yaml")

        route("/items") {
            get {
                call.respond(itemsList)
            }
            post {
                val body = call.attributes[BodyAttributeKey]
                val newItem = jacksonObjectMapper().readValue(body, Item::class.java)
                itemsList.add(newItem)
                call.respond(HttpStatusCode.Created, newItem)
            }
        }
    }
}

val BodyAttributeKey = AttributeKey<String>("RequestBody")
