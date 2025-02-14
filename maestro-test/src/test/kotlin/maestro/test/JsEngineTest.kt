package maestro.test

import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.stubFor
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo
import com.github.tomakehurst.wiremock.junit5.WireMockTest
import com.google.common.truth.Truth.assertThat
import maestro.js.JsEngine
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mozilla.javascript.RhinoException

@WireMockTest
abstract class JsEngineTest {

    lateinit var engine: JsEngine

    @Test
    fun `HTTP - Make GET request`(wiremockInfo: WireMockRuntimeInfo) {
        // Given
        val port = wiremockInfo.httpPort
        stubFor(
            get("/json").willReturn(
                okJson(
                    """
                        {
                            "message": "GET Endpoint"
                        }
                    """.trimIndent()
                )
            )
        )

        val script = """
            var response = http.get('http://localhost:$port/json')
            
            json(response.body).message
        """.trimIndent()

        // When
        val result = engine.evaluateScript(script)

        // Then
        assertThat(result.toString()).isEqualTo("GET Endpoint")
    }

    @Test
    fun `HTTP - Make GET request with headers`(wiremockInfo: WireMockRuntimeInfo) {
        // Given
        val port = wiremockInfo.httpPort
        stubFor(
            get("/json")
                .withHeader("Authorization", equalTo("Bearer Token"))
                .willReturn(
                    okJson(
                        """
                            {
                                "message": "GET Endpoint with auth"
                            }
                        """.trimIndent()
                    )
                )
        )

        val script = """
            var response = http.get('http://localhost:$port/json', {
                headers: {
                    Authorization: 'Bearer Token'
                }
            })
            
            json(response.body).message
        """.trimIndent()

        // When
        val result = engine.evaluateScript(script)

        // Then
        assertThat(result.toString()).isEqualTo("GET Endpoint with auth")
    }

    @Test
    fun `HTTP - Make POST request`(wiremockInfo: WireMockRuntimeInfo) {
        // Given
        val port = wiremockInfo.httpPort
        stubFor(
            post("/json")
                .withRequestBody(
                    equalToJson(
                        """
                            {
                                "payload": "Value"
                            }
                        """.trimIndent()
                    )
                )
                .willReturn(
                    okJson(
                        """
                            {
                                "message": "POST endpoint"
                            }
                        """.trimIndent()
                    )
                )
        )

        val script = """
            var response = http.post('http://localhost:$port/json', {
                body: JSON.stringify(
                    {
                        payload: 'Value'
                    }
                )
            })
            
            json(response.body).message
        """.trimIndent()

        // When
        val result = engine.evaluateScript(script)

        // Then
        assertThat(result.toString()).isEqualTo("POST endpoint")
    }

    @Test
    fun `Allow sharing output object between scripts`() {
        engine.evaluateScript("output.foo = 'foo'")
        val foo = engine.evaluateScript("output.foo")
        assertThat(foo.toString()).isEqualTo("foo")
    }

    @Test
    fun `Undeclared variables are falsy`() {
        val result = engine.evaluateScript("!!foo").toString()
        assertThat(result).isEqualTo("false")
    }

    @Test
    fun `Environment variables are accessible across scopes`() {
        engine.putEnv("FOO", "foo")

        var result = engine.evaluateScript("FOO").toString()
        assertThat(result).isEqualTo("foo")

        engine.enterScope()

        result = engine.evaluateScript("FOO").toString()
        assertThat(result).isEqualTo("foo")
    }

    @Test
    fun `Inline environment variables are accessible across scopes`() {
        var result = engine.evaluateScript("FOO", env = mapOf("FOO" to "foo")).toString()
        assertThat(result).isEqualTo("foo")

        result = engine.evaluateScript("FOO").toString()
        assertThat(result).isEqualTo("foo")

        engine.enterScope()

        result = engine.evaluateScript("FOO").toString()
        assertThat(result).isEqualTo("foo")
    }
}