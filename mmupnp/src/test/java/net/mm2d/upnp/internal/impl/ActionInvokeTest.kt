/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.impl

import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import net.mm2d.upnp.*
import net.mm2d.upnp.util.XmlUtils
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.IOException
import java.net.URL
import java.util.*
import javax.xml.transform.TransformerException

@Suppress("TestFunctionName", "NonAsciiCharacters")
@RunWith(JUnit4::class)
class ActionInvokeTest {
    private lateinit var httpResponse: HttpResponse
    private lateinit var url: URL
    private lateinit var action: ActionImpl
    private lateinit var mockHttpClient: HttpClient

    @Before
    fun setUp() {
        url = URL("http://127.0.0.1:8888/test")
        val service: ServiceImpl = mockk(relaxed = true)
        every { service.serviceType } returns SERVICE_TYPE
        every { service.controlUrl } returns ""
        action = spyk(
            ActionImpl.Builder()
                .setService(service)
                .setName(ACTION_NAME)
                .addArgumentBuilder(
                    ArgumentImpl.Builder()
                        .setName(IN_ARG_NAME_1)
                        .setDirection("in")
                        .setRelatedStateVariableName("1")
                        .setRelatedStateVariable(
                            StateVariableImpl.Builder()
                                .setDataType("string")
                                .setName("1")
                                .build()
                        )
                )
                .addArgumentBuilder(
                    ArgumentImpl.Builder()
                        .setName(IN_ARG_NAME_2)
                        .setDirection("in")
                        .setRelatedStateVariableName("2")
                        .setRelatedStateVariable(
                            StateVariableImpl.Builder()
                                .setDataType("string")
                                .setName("2")
                                .setDefaultValue(IN_ARG_DEFAULT_VALUE)
                                .build()
                        )
                )
                .addArgumentBuilder(
                    ArgumentImpl.Builder()
                        .setName(OUT_ARG_NAME1)
                        .setDirection("out")
                        .setRelatedStateVariableName("3")
                        .setRelatedStateVariable(
                            StateVariableImpl.Builder()
                                .setDataType("string")
                                .setName("3")
                                .build()
                        )
                )
                .build()
        ) as ActionImpl
        every { action.makeAbsoluteControlUrl() } returns url
        mockHttpClient = spyk(HttpClient())
        httpResponse = HttpResponse.create()
        httpResponse.setStatus(Http.Status.HTTP_OK)
        httpResponse.setBody(
            "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\""
                    + " s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">\n"
                    + "<s:Body>\n"
                    + "<u:" + ACTION_NAME + "Response xmlns:u=\"" + SERVICE_TYPE + "\">\n"
                    + "<" + OUT_ARG_NAME1 + ">" + OUT_ARG_VALUE1 + "</" + OUT_ARG_NAME1 + ">\n"
                    + "</u:" + ACTION_NAME + "Response>\n"
                    + "</s:Body>\n"
                    + "</s:Envelope>"
        )
        every { action.createHttpClient() } returns mockHttpClient
    }

    @Test(expected = IOException::class)
    fun invoke_postでIOExceptionが発生() {
        val client: HttpClient = mockk(relaxed = true)
        every { client.post(any()) } throws IOException()
        every { action.createHttpClient() } returns client
        action.invokeSync(emptyMap())
    }

    @Test
    fun invoke_リクエストヘッダの確認() {
        val slot = slot<HttpRequest>()
        every { mockHttpClient.post(capture(slot)) } returns httpResponse
        action.invokeSync(emptyMap())

        val request = slot.captured
        assertThat(request.getMethod()).isEqualTo("POST")
        assertThat(request.getUri()).isEqualTo(url.path)
        assertThat(request.version).isEqualTo("HTTP/1.1")
        assertThat(request.getHeader(Http.HOST)).isEqualTo(url.host + ":" + url.port)
        assertThat<String>(request.getHeader(Http.CONTENT_LENGTH))
            .isEqualTo(request.getBodyBinary()?.size.toString())
    }

    private fun createChildElementList(parent: Element): List<Element> {
        val elements = ArrayList<Element>()
        val children = parent.childNodes
        for (i in 0 until children.length) {
            val child = children.item(i)
            if (child.nodeType == Node.ELEMENT_NODE) {
                elements.add(child as Element)
            }
        }
        return elements
    }

    @Test
    fun invoke_リクエストSOAPフォーマットの確認() {
        val slot = slot<HttpRequest>()
        every { mockHttpClient.post(capture(slot)) } returns httpResponse
        action.invokeSync(emptyMap())
        val request = slot.captured
        val doc = XmlUtils.newDocument(true, request.getBody()!!)
        val envelope = doc.documentElement

        assertThat(envelope.localName).isEqualTo("Envelope")
        assertThat(envelope.namespaceURI).isEqualTo(SOAP_NS)
        assertThat(envelope.getAttributeNS(SOAP_NS, "encodingStyle")).isEqualTo(SOAP_STYLE)

        val body = XmlUtils.findChildElementByLocalName(envelope, "Body")

        assertThat(body).isNotNull()
        assertThat(body!!.namespaceURI).isEqualTo(SOAP_NS)

        val action = XmlUtils.findChildElementByLocalName(body, ACTION_NAME)

        assertThat(action).isNotNull()
        assertThat(action!!.namespaceURI).isEqualTo(SERVICE_TYPE)
    }

    @Test
    fun invoke_リクエストSOAPの引数確認_指定なしでの実行() {
        val slot = slot<HttpRequest>()
        every { mockHttpClient.post(capture(slot)) } returns httpResponse
        action.invokeSync(emptyMap())
        val request = slot.captured

        val doc = XmlUtils.newDocument(true, request.getBody()!!)
        val envelope = doc.documentElement
        val body = XmlUtils.findChildElementByLocalName(envelope, "Body")
        val action = XmlUtils.findChildElementByLocalName(body!!, ACTION_NAME)

        val elements = createChildElementList(action!!)
        assertThat(elements).hasSize(2)
        assertThat(elements[0].localName).isEqualTo(IN_ARG_NAME_1)
        assertThat(elements[0].textContent).isEqualTo("")

        assertThat(elements[1].localName).isEqualTo(IN_ARG_NAME_2)
        assertThat(elements[1].textContent).isEqualTo(IN_ARG_DEFAULT_VALUE)
    }

    @Test
    fun invoke_リクエストSOAPの引数確認_指定ありでの実行() {
        val value1 = "value1"
        val value2 = "value2"
        val slot = slot<HttpRequest>()
        every { mockHttpClient.post(capture(slot)) } returns httpResponse
        val argument = mapOf(
            IN_ARG_NAME_1 to value1,
            IN_ARG_NAME_2 to value2
        )
        action.invokeSync(argument)
        val request = slot.captured

        val doc = XmlUtils.newDocument(true, request.getBody()!!)
        val envelope = doc.documentElement
        val body = XmlUtils.findChildElementByLocalName(envelope, "Body")
        val action = XmlUtils.findChildElementByLocalName(body!!, ACTION_NAME)

        val elements = createChildElementList(action!!)
        assertThat(elements).hasSize(2)
        assertThat(elements[0].localName).isEqualTo(IN_ARG_NAME_1)
        assertThat(elements[0].textContent).isEqualTo(value1)

        assertThat(elements[1].localName).isEqualTo(IN_ARG_NAME_2)
        assertThat(elements[1].textContent).isEqualTo(value2)
    }

    @Test
    fun invoke_リクエストSOAPの引数確認_カスタム引数指定NSなし() {
        val value1 = "value1"
        val value2 = "value2"
        val name = "name"
        val value = "value"
        val slot = slot<HttpRequest>()
        every { mockHttpClient.post(capture(slot)) } returns httpResponse
        val argument = mapOf(
            IN_ARG_NAME_1 to value1,
            IN_ARG_NAME_2 to value2
        )
        action.invokeCustomSync(argument, customArguments = Collections.singletonMap(name, value))
        val request = slot.captured

        val doc = XmlUtils.newDocument(true, request.getBody()!!)
        val envelope = doc.documentElement
        val body = XmlUtils.findChildElementByLocalName(envelope, "Body")
        val action = XmlUtils.findChildElementByLocalName(body!!, ACTION_NAME)

        val elements = createChildElementList(action!!)
        assertThat(elements.size).isEqualTo(3)
        assertThat(elements[0].localName).isEqualTo(IN_ARG_NAME_1)
        assertThat(elements[0].textContent).isEqualTo(value1)

        assertThat(elements[1].localName).isEqualTo(IN_ARG_NAME_2)
        assertThat(elements[1].textContent).isEqualTo(value2)

        assertThat(elements[2].localName).isEqualTo(name)
        assertThat(elements[2].textContent).isEqualTo(value)
    }

    @Test
    fun invoke_リクエストSOAPの引数確認_カスタム引数指定NSあり() {
        val value1 = "value1"
        val value2 = "value2"
        val prefix = "custom"
        val urn = "urn:schemas-custom-com:custom"
        val name = "name"
        val value = "value"
        val slot = slot<HttpRequest>()
        every { mockHttpClient.post(capture(slot)) } returns httpResponse
        val argument = mapOf(
            IN_ARG_NAME_1 to value1,
            IN_ARG_NAME_2 to value2
        )

        action.invokeCustomSync(
            argument,
            customNamespace = Collections.singletonMap(prefix, urn),
            customArguments = Collections.singletonMap("$prefix:$name", value)
        )
        val request = slot.captured

        val doc = XmlUtils.newDocument(true, request.getBody()!!)
        val envelope = doc.documentElement
        val body = XmlUtils.findChildElementByLocalName(envelope, "Body")
        val action = XmlUtils.findChildElementByLocalName(body!!, ACTION_NAME)

        val elements = createChildElementList(action!!)
        assertThat(elements.size).isEqualTo(3)
        assertThat(elements[0].localName).isEqualTo(IN_ARG_NAME_1)
        assertThat(elements[0].textContent).isEqualTo(value1)

        assertThat(elements[1].localName).isEqualTo(IN_ARG_NAME_2)
        assertThat(elements[1].textContent).isEqualTo(value2)

        assertThat(elements[2].localName).isEqualTo(name)
        assertThat(elements[2].textContent).isEqualTo(value)
    }

    @Test
    fun invoke_200以外のレスポンスでIOExceptionが発生() {
        var statusCount = 0
        var exceptionCount = 0
        for (status in Http.Status.values()) {
            httpResponse.setStatus(status)
            every { mockHttpClient.post(any()) } returns httpResponse
            if (status === Http.Status.HTTP_OK) {
                action.invokeSync(emptyMap())
                continue
            }
            try {
                statusCount++
                action.invokeSync(emptyMap())
            } catch (ignored: IOException) {
                exceptionCount++
            }

        }
        assertThat(statusCount).isEqualTo(exceptionCount)
    }

    @Test(expected = IOException::class)
    fun invoke_BodyタグがないとIOExceptionが発生() {
        httpResponse.setBody(
            "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\""
                    + " s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">\n"
                    + "</s:Envelope>"
        )
        every { mockHttpClient.post(any()) } returns httpResponse

        action.invokeSync(emptyMap(), false)
    }

    @Test(expected = IOException::class)
    fun invoke_ActionタグがないとIOExceptionが発生() {
        httpResponse.setBody(
            "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\""
                    + " s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">\n"
                    + "<s:Body>\n"
                    + "</s:Body>\n"
                    + "</s:Envelope>"
        )
        every { mockHttpClient.post(any()) } returns httpResponse

        action.invokeSync(emptyMap(), false)
    }

    @Test
    fun invoke_実行結果をパースしMapとして戻ること() {
        every { mockHttpClient.post(any()) } returns httpResponse
        val result = action.invokeSync(emptyMap())
        assertThat(result[OUT_ARG_NAME1]).isEqualTo(OUT_ARG_VALUE1)
    }

    @Test
    fun invoke_argumentListにない結果が含まれていても結果に含まれる() {
        httpResponse.setBody(
            "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\""
                    + " s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">"
                    + "<s:Body>\n"
                    + "<u:" + ACTION_NAME + "Response xmlns:u=\"" + SERVICE_TYPE + "\">\n"
                    + "<" + OUT_ARG_NAME1 + ">" + OUT_ARG_VALUE1 + "</" + OUT_ARG_NAME1 + ">\n"
                    + "<" + OUT_ARG_NAME2 + ">" + OUT_ARG_VALUE2 + "</" + OUT_ARG_NAME2 + ">\n"
                    + "</u:" + ACTION_NAME + "Response>\n"
                    + "</s:Body>\n"
                    + "</s:Envelope>"
        )
        every { mockHttpClient.post(any()) } returns httpResponse
        val result = action.invokeSync(emptyMap())
        assertThat(result[OUT_ARG_NAME1]).isEqualTo(OUT_ARG_VALUE1)
        assertThat(result).containsKey(OUT_ARG_NAME2)
    }

    @Test(expected = IOException::class)
    fun invoke_エラーレスポンスのときIOExceptionが発生() {
        httpResponse.setStatus(Http.Status.HTTP_INTERNAL_ERROR)
        httpResponse.setBody(ERROR_RESPONSE)
        every { mockHttpClient.post(any()) } returns httpResponse
        action.invokeSync(emptyMap(), false)
    }

    @Test(expected = IOException::class)
    fun invoke_エラーレスポンスにerrorCodeがないとIOExceptionが発生() {
        httpResponse.setStatus(Http.Status.HTTP_INTERNAL_ERROR)
        httpResponse.setBody(
            "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" "
                    + "s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">\n"
                    + "<s:Body>\n"
                    + "<s:Fault>\n"
                    + "<faultcode>s:Client</faultcode>\n"
                    + "<faultstring>UPnPError</faultstring>\n"
                    + "<detail>\n"
                    + "<UPnPError xmlns=\"urn:schemas-upnp-org:control-1-0\">\n"
                    + "<errorDescription>Restricted object</errorDescription>\n"
                    + "</UPnPError>\n"
                    + "</detail>\n"
                    + "</s:Fault>\n"
                    + "</s:Body>\n"
                    + "</s:Envelope>"
        )
        every { mockHttpClient.post(any()) } returns httpResponse
        action.invokeSync(emptyMap(), true)
    }

    @Test(expected = IOException::class)
    fun invoke_エラーレスポンスにUPnPErrorがないとIOExceptionが発生() {
        httpResponse.setStatus(Http.Status.HTTP_INTERNAL_ERROR)
        httpResponse.setBody(
            "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" "
                    + "s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">\n"
                    + "<s:Body>\n"
                    + "<s:Fault>\n"
                    + "<faultcode>s:Client</faultcode>\n"
                    + "<faultstring>UPnPError</faultstring>\n"
                    + "<detail>\n"
                    + "</detail>\n"
                    + "</s:Fault>\n"
                    + "</s:Body>\n"
                    + "</s:Envelope>"
        )
        every { mockHttpClient.post(any()) } returns httpResponse
        action.invokeSync(emptyMap(), true)
    }

    @Test(expected = IOException::class)
    fun invoke_エラーレスポンスにBodyがないとIOExceptionが発生() {
        httpResponse.setStatus(Http.Status.HTTP_INTERNAL_ERROR)
        httpResponse.setBody(
            "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" "
                    + "s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">\n"
                    + "</s:Envelope>"
        )
        every { mockHttpClient.post(any()) } returns httpResponse
        action.invokeSync(emptyMap(), true)
    }

    @Test(expected = IOException::class)
    fun invoke_エラーレスポンスにFaultがないとIOExceptionが発生() {
        httpResponse.setStatus(Http.Status.HTTP_INTERNAL_ERROR)
        httpResponse.setBody(
            "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" "
                    + "s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">\n"
                    + "<s:Body>\n"
                    + "</s:Body>\n"
                    + "</s:Envelope>"
        )
        every { mockHttpClient.post(any()) } returns httpResponse
        action.invokeSync(emptyMap(), true)
    }

    @Test
    fun invoke_エラーレスポンスもパースできる() {
        httpResponse.setStatus(Http.Status.HTTP_INTERNAL_ERROR)
        httpResponse.setBody(ERROR_RESPONSE)
        every { mockHttpClient.post(any()) } returns httpResponse
        val result = action.invokeSync(emptyMap(), true)
        assertThat(result[Action.FAULT_CODE_KEY]).isEqualTo("s:Client")
        assertThat(result[Action.FAULT_STRING_KEY]).isEqualTo("UPnPError")
        assertThat(result[Action.ERROR_CODE_KEY]).isEqualTo("711")
        assertThat(result[Action.ERROR_DESCRIPTION_KEY]).isEqualTo("Restricted object")
    }

    @Test(expected = IOException::class)
    fun invoke_エラーレスポンスのときIOExceptionが発生2() {
        httpResponse.setStatus(Http.Status.HTTP_INTERNAL_ERROR)
        httpResponse.setBody(ERROR_RESPONSE)
        every { mockHttpClient.post(any()) } returns httpResponse
        action.invokeSync(emptyMap())
    }

    @Test
    fun invoke_エラーレスポンスもパースできる2() {
        httpResponse.setStatus(Http.Status.HTTP_INTERNAL_ERROR)
        httpResponse.setBody(ERROR_RESPONSE)
        every { mockHttpClient.post(any()) } returns httpResponse
        val result = action.invokeSync(emptyMap(), true)
        assertThat(result[Action.FAULT_CODE_KEY]).isEqualTo("s:Client")
        assertThat(result[Action.FAULT_STRING_KEY]).isEqualTo("UPnPError")
        assertThat(result[Action.ERROR_CODE_KEY]).isEqualTo("711")
        assertThat(result[Action.ERROR_DESCRIPTION_KEY]).isEqualTo("Restricted object")
    }

    @Test(expected = IOException::class)
    fun invoke_ステータスコードがOKで中身が空のときIOExceptionが発生() {
        httpResponse.setStatus(Http.Status.HTTP_OK)
        httpResponse.setBody("")
        every { mockHttpClient.post(any()) } returns httpResponse
        action.invokeSync(emptyMap(), true)
    }

    @Test(expected = IOException::class)
    fun invoke_ステータスコードがエラーで中身が空のときIOExceptionが発生() {
        httpResponse.setStatus(Http.Status.HTTP_INTERNAL_ERROR)
        httpResponse.setBody("")
        every { mockHttpClient.post(any()) } returns httpResponse
        action.invokeSync(emptyMap(), true)
    }

    @Test(expected = IOException::class)
    fun invoke_ステータスコードがその他で中身が空のときIOExceptionが発生() {
        httpResponse.setStatus(Http.Status.HTTP_NOT_FOUND)
        httpResponse.setBody("")
        every { mockHttpClient.post(any()) } returns httpResponse
        action.invokeSync(emptyMap(), true)
    }

    @Test(expected = IOException::class)
    fun invoke_ステータスコードがOKで中身がxmlとして異常のときIOExceptionが発生() {
        httpResponse.setStatus(Http.Status.HTTP_OK)
        httpResponse.setBody("<>")
        every { mockHttpClient.post(any()) } returns httpResponse
        action.invokeSync(emptyMap(), true)
    }

    @Test(expected = IOException::class)
    fun invoke_ステータスコードがエラーで中身がxmlとして異常のときIOExceptionが発生() {
        httpResponse.setStatus(Http.Status.HTTP_INTERNAL_ERROR)
        httpResponse.setBody("<>")
        every { mockHttpClient.post(any()) } returns httpResponse
        action.invokeSync(emptyMap(), true)
    }

    @Test(expected = IOException::class)
    fun makeSoap_xml作成でExceptionが発生したらIOException() {
        every { action.formatXmlString(any()) } throws TransformerException("")
        action.makeSoap(emptyMap(), emptyList())
    }

    companion object {
        private const val SOAP_NS = "http://schemas.xmlsoap.org/soap/envelope/"
        private const val SOAP_STYLE = "http://schemas.xmlsoap.org/soap/encoding/"
        private const val IN_ARG_NAME_1 = "InArgName1"
        private const val IN_ARG_NAME_2 = "InArgName2"
        private const val IN_ARG_DEFAULT_VALUE = "Default"
        private const val OUT_ARG_NAME1 = "OutArgName1"
        private const val OUT_ARG_VALUE1 = "OutArgValue1"
        private const val OUT_ARG_NAME2 = "OutArgName2"
        private const val OUT_ARG_VALUE2 = "OutArgValue2"
        private const val ACTION_NAME = "TestAction"
        private const val SERVICE_TYPE = "urn:schemas-upnp-org:service:TestServiceType:1"

        private const val ERROR_RESPONSE = (
                "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" "
                        + "s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">\n"
                        + "<s:Body>\n"
                        + "<s:Fault>\n"
                        + "<faultcode>s:Client</faultcode>\n"
                        + "<faultstring>UPnPError</faultstring>\n"
                        + "<detail>\n"
                        + "<UPnPError xmlns=\"urn:schemas-upnp-org:control-1-0\">\n"
                        + "<errorCode>711</errorCode>\n"
                        + "<errorDescription>Restricted object</errorDescription>\n"
                        + "</UPnPError>\n"
                        + "</detail>\n"
                        + "</s:Fault>\n"
                        + "</s:Body>\n"
                        + "</s:Envelope>")
    }
}
