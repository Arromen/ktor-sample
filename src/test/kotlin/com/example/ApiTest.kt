package com.example

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.example.module
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import org.slf4j.LoggerFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ApiTest {

    @Test
    fun testGetItems() = testApplication {
        application { module() }

        val response: HttpResponse = client.get("/items")
        assertEquals(HttpStatusCode.Companion.OK, response.status)
        assertTrue(response.bodyAsText().contains("Phone"))
    }

    @Test
    fun testPostItem() = testApplication {
        application { module() }

        val response: HttpResponse = client.post("/items") {
            contentType(ContentType.Application.Json)
            setBody("""{"id": 3, "name": "Tablet"}""")
        }

        assertEquals(HttpStatusCode.Companion.Created, response.status)
        assertTrue(response.bodyAsText().contains("Tablet"))
    }

    @Test
    fun testInvalidRouteReturns404() = testApplication {
        application { module() }

        val response: HttpResponse = client.get("/unknown")
        assertEquals(HttpStatusCode.Companion.NotFound, response.status)
    }

    @Test
    fun testMiddlewareLogsRequestBody() = testApplication {
        // Подключаем ListAppender для перехвата логов
        val logger = LoggerFactory.getLogger("CustomMiddleware") as Logger
        val listAppender = ListAppender<ILoggingEvent>()
        listAppender.start()
        logger.addAppender(listAppender)

        application { module() }

        client.post("/items") {
            contentType(ContentType.Application.Json)
            setBody("""{"id": 99, "name": "MiddlewareTest"}""")
        }

        val logs = listAppender.list.joinToString { it.formattedMessage }
        assertTrue(logs.contains("MiddlewareTest"), "Логи должны содержать тело запроса")
    }
}