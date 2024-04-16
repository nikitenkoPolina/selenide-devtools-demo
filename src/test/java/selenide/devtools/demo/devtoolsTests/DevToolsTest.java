package selenide.devtools.demo.devtoolsTests;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.openqa.selenium.WebDriverException;

import org.openqa.selenium.devtools.DevTools;

import org.openqa.selenium.devtools.v120.emulation.Emulation;

import org.openqa.selenium.devtools.v120.log.Log;
import org.openqa.selenium.devtools.v120.log.model.LogEntry;
import org.openqa.selenium.devtools.v120.network.Network;
import org.openqa.selenium.devtools.v120.network.model.*;
import org.openqa.selenium.devtools.v120.performance.Performance;
import org.openqa.selenium.devtools.v120.performance.model.Metric;

import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import selenide.devtools.demo.ServiceTest;
import selenide.devtools.demo.extensions.EnvironmentExtension;
import selenide.devtools.demo.paramresolver.LoggingParamResolver;
import selenide.devtools.demo.service.DevToolsService;

import java.time.Duration;
import java.util.*;
import java.util.stream.Stream;

import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Selenide.*;
import static com.codeborne.selenide.WebDriverRunner.getWebDriver;
import static org.apache.hc.core5.http.HttpStatus.SC_OK;
import static org.apache.hc.core5.http.Method.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.RepeatedTest.SHORT_DISPLAY_NAME;
import static org.openqa.selenium.devtools.v120.network.model.ResourceType.DOCUMENT;

@Tag("full")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
//@TestMethodOrder(MethodOrderer.DisplayName.class)
@ExtendWith({LoggingParamResolver.class})
public class DevToolsTest extends ServiceTest {

    //final Logger logger = LoggerFactory.getLogger(DevToolsTest.class);

    private final String URL = "https://mellonis.github.io/react-burger/";
    private DevTools devTools;
    private Logger logger;

    static Stream<Arguments> getArgumentsForHeadersTest() {
        return Stream.of(
                Arguments.of("testCustomHeader", "customHeaderValue"),
                Arguments.of("1", "customHeaderValue"),
                Arguments.of("testCustomHeader", "")
        );
    }

    @BeforeEach
    public void createSession(DevToolsService chromeDevTools, Logger logger) {

        this.logger = logger;

        logger.info("Подключение к сессии Devtools и получение объекта devTools");
        this.devTools = chromeDevTools.connectDTools();

    }

    @Test
    @Tag("performance")
    @DisplayName("Performance: Получение информации о метриках")
    void getPerformanceMetrics() {

        logger.info("Получаем доступ к метрикам");
        devTools.send(Performance.enable(Optional.empty()));

        logger.info("Возвращаем список объектов типа Metric");
        List<Metric> metrics = devTools.send(Performance.getMetrics());
        // затем его можно перебирать, чтобы получить информацию о каждой метрике производительности

        // for (Metric metric: metrics) {
//            System.out.println(metric.getName() + ": " + metric.getValue());
//        }

        //metrics.forEach(metric -> System.out.printf("%s: %s%n", metric.getName(), metric.getValue()));

        open(URL);
        Map<String, Number> map = new HashMap<>();

        for (Metric metric : metrics) {
            map.put(metric.getName(), metric.getValue());
        }

        assertThat(map).isNotEmpty();
    }

    @RepeatedTest(value = 3, name = SHORT_DISPLAY_NAME)
    @Tag("emulation")
    @DisplayName("Location: Эмуляция геолокации")
    void emulateGeolocation() {

        String mapsUrl = "https://www.google.com/maps/";
        Number latitude = 19.2542;
        Number longitude = 99.0739;

        devTools.send(Emulation.setGeolocationOverride(
                Optional.of(latitude), Optional.of(longitude), Optional.of(1))
        );

        open(mapsUrl);

        $("[id='mylocation']").shouldBe(enabled).click();
        // waitForPageUpdate();

        logger.info("Проверка, что ожидание загрузки не дольше 1.1 секунды");
        assertTimeout(Duration.ofMillis(1100L), this::waitForPageUpdate);

        logger.info("Проверка, что в Url подставились новые значения широты и долготы");
        assertAll(
                () -> assertTrue(getWebDriver().getCurrentUrl().contains(String.valueOf(latitude))),
                () -> assertTrue(getWebDriver().getCurrentUrl().contains(String.valueOf(longitude)))
        );
    }

    @Test
    @Tag("emulation")
    @DisplayName("Network: Эмуляция условий сети")
    void emulateNetworkConditionsTest() {

        devTools.send(Network.enable(Optional.empty(),
                Optional.empty(),
                Optional.empty()));

        devTools.send(Network.emulateNetworkConditions(false,
                1000, 1000000, 1000000, Optional.of(ConnectionType.CELLULAR3G)));

        open("https://www.youtube.com");
    }

    // пример действия @ValueSource
    @ParameterizedTest
    @Tag("emulation")
    @DisplayName("Network: emulate offline")
    @ValueSource(booleans = {true, false})
    public void emulateNetworkOfflineTest(boolean offline) {

        devTools.send(Network.enable(
                Optional.empty(), Optional.empty(), Optional.empty()));
        devTools.send(Network.emulateNetworkConditions(
                offline, 100, 200000, 100000, Optional.of(ConnectionType.CELLULAR3G)));

        Map<Integer, String> message = new HashMap<>();

        devTools.addListener(Network.loadingFailed(), loadingFailed -> {
//            String loadingFailedErrorText = loadingFailed.getErrorText();
//            int count = message.containsValue(loadingFailedErrorText) ? message.values().size() : 0;
//                message.put(count + 1, loadingFailed.getErrorText());

            message.put(message.values().size(), loadingFailed.getErrorText());
        });

        Exception exception = assertThrows(WebDriverException.class, () -> open(URL));

        logger.info("Проверка текста ошибки выброшенного исключения");
        assertThat(exception.getMessage()).contains("unknown error: net::ERR_INTERNET_DISCONNECTED");

        logger.info("Проверка текста ошибки через Event<LoadingFailed>");
        assertThat(message).containsValue("net::ERR_INTERNET_DISCONNECTED");
        message.forEach((k, v) -> assertEquals("net::ERR_INTERNET_DISCONNECTED", message.get(k)));
    }


    @Test
    @Tag("request")
    @DisplayName("Request: анализ параметров запроса")
    void getRequestTest() {

        Map<String, RequestWillBeSent> requests = new HashMap<>();
        devTools.send(Network.enable(Optional.empty(),
                Optional.empty(),
                Optional.empty()));

        devTools.addListener(
                Network.requestWillBeSent(),
                request -> {
                    if (request.getRequest().getUrl().equals(URL)) {
                        System.out.println("-------------------------------------------");
                        System.out.println("Request Method : " + request.getRequest().getMethod());
                        System.out.println("Request URL : " + request.getRequest().getUrl());
                        System.out.println("Request headers: " + request.getRequest().getHeaders().toString());
                        System.out.println("Request body: " + request.getRequest().getPostData().toString());
                        System.out.println("-------------------------------------------");

                        requests.put(request.getRequest().getUrl(), request);
                    }
                }
        );

        open(URL);

        RequestWillBeSent requestWillBeSent = requests.get(URL);

        assertAll(
                () -> assertEquals(GET.toString(), requestWillBeSent.getRequest().getMethod()),
                () -> requestWillBeSent.getType().ifPresent(
                        type -> assertThat(requestWillBeSent.getType().get().toString()).isEqualTo(DOCUMENT.toString()))
        );

//        assertAll("Grouped Assertions of requests map",
//                () -> requests.keySet().forEach(k -> assertFalse((requests.get(k).isEmpty()), "Target url is empty: " + k)),
//                () -> requests.forEach((k, v) -> assertEquals(GET.toString(), requests.get(k))),
//                () -> requests.values().forEach(v -> assertNotNull(v, "Request without method is found: " + v))
//        );
    }

    @Test
    @Tag("response")
    @DisplayName("Response: анализ параметров ответа")
    void getResponseTest() {

        devTools.send(Network.enable(Optional.empty(), Optional.empty(), Optional.empty()));

        Map<String, ResponseReceived> responses = new HashMap<>();
        devTools.addListener(
                Network.responseReceived(),
                response -> {
                    if (response.getResponse().getUrl().equals(URL)) {
                        String responseUrl = response.getResponse().getUrl();
                        System.out.println("-------------------------------------------");
                        System.out.println("Url: " + responseUrl);
                        System.out.println("Response headers: " + response.getResponse().getHeaders().toString());
                        System.out.println("Response status: " + response.getResponse().getStatus());
                        System.out.println("-------------------------------------------");

                        responses.put(responseUrl, response);
                    }
                }
        );

        open(URL);

        ResponseReceived responseReceived = responses.get(URL);

        Map<String, Object> headers = responseReceived.getResponse().getHeaders();
        System.out.println(headers);

        assertEquals(SC_OK, responseReceived.getResponse().getStatus());
        assertEquals(headers.get("content-type"), responseReceived.getResponse().getMimeType() + "; charset=utf-8");
        assertEquals("bytes", headers.get("accept-ranges"));

//
//        $(By.xpath("//*[@href='/lib/']"))
//                .shouldBe(visible)
//                .click();
//
//        responses.forEach((k, v) ->
//                assertEquals(SC_OK, responses.get(k), String.format("Status code is not 200 OK: %s %s ", k, v)
//                )
//        );
    }

    @Test
    @Tag("response")
    @DisplayName("Response: анализ тела ответа")
    void getResponseBodyTest() {

        devTools.send(Network.enable(Optional.empty(), Optional.empty(), Optional.empty()));

        Map<Response, String> responses = new HashMap<>();
        devTools.addListener(Network.responseReceived(), response -> {
            String responseUrl = response.getResponse().getUrl();
            RequestId requestId = response.getRequestId();
            String body = devTools.send(Network.getResponseBody(requestId)).getBody();

            if (responseUrl.equals("https://www.penoplex.ru/lib/")) {
//                System.out.println("Url: " + responseUrl);
//                System.out.println("Response body: " + body);
                responses.put(response.getResponse(), body);
            }
        });

        open("https://www.penoplex.ru/lib/");
        System.out.println(responses);

        assertAll("Grouped Assertions of response",
                () -> responses.forEach((response, body) -> assertNotNull(body)),
                () -> responses.forEach((response, body) -> assertTrue(response.getResponseTime().isPresent()))
        );
    }

    @Test
    @Tag("response")
    @DisplayName("Response: получение времени ответа на запрос")
    void getResponseTimingTest() {

        devTools.send(Network.enable(Optional.empty(), Optional.empty(), Optional.empty()));

        Map<String, Double> timingMap = new HashMap<>();
        Map<String, String> requestIdMap = new HashMap<>();

        Map<String, Double> responseTimeMap = new HashMap<>();
        devTools.addListener(Network.requestWillBeSent(), requestWillBeSent -> {
            if (requestWillBeSent.getRequest().getUrl().equals(URL)) {

                logger.info("Получаем requestId запроса");
                RequestId requestId = requestWillBeSent.getRequestId();

                logger.info("Получаем время начала запроса");
                double startTime = requestWillBeSent.getTimestamp().toJson().doubleValue();

                logger.info("Получаем url-адрес запроса");
                String requestUrl = requestWillBeSent.getRequest().getUrl();

                timingMap.put(requestId.toString(), startTime);
                requestIdMap.put(requestId.toString(), requestUrl);
            }
        });

        devTools.addListener(Network.loadingFinished(), loadingFinished -> {

            RequestId requestId = loadingFinished.getRequestId();

            logger.info("Получаем время завершения запроса");
            double endTime = loadingFinished.getTimestamp().toJson().doubleValue();

            if (timingMap.get(requestId.toString()) != null) {

                String urlReq = requestIdMap.get(requestId.toString());

                logger.info("Находим время ответа на запрос");
                double responseTime = endTime - timingMap.get(requestId.toString());

                System.out.println("Url: " + urlReq + " , response time: " + responseTime + " seconds");

                responseTimeMap.put(urlReq, responseTime);

            }
        });

        open(URL);

        assertTrue(2 > responseTimeMap.get(URL));
    }


    @Test
    @Tag("emulation")
    @DisplayName("Подмена userAgent")
    public void fakeUserAgentTest() {

        String mAgent = "Mozilla/5.0 (Linux; Android 8.0.0; SM-G955U Build/R16NW) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/537.36";
        devTools.send(Network.enable(
                Optional.empty(), Optional.empty(), Optional.empty()));
        devTools.send(Network.setUserAgentOverride(mAgent,
                Optional.empty(), Optional.empty(), Optional.empty()));
        open("https://www.imdb.com/");
        assertEquals("https://m.imdb.com/", getWebDriver().getCurrentUrl(), "Opened imdb version is not mobile");
    }

    //@Disabled
    @Test
    @Tag("emulation")
    @DisplayName("Эмуляция мобильного устройства")
    void emulateMobileTest() {

    }

    private void waitForPageUpdate() {
        new WebDriverWait(getWebDriver(),
                Duration.ofMillis(4000)).until(ExpectedConditions.urlContains("/@"));
    }

    @Nested
    @DisplayName("Проверка сообщений вкладки Console")
    @Tag("console")
    class ConsoleTest {

        @Test
        @DisplayName("Console: Получение логов")
        void getConsoleLogs() {

            logger.info("Получим доступ к журналу логов в консоли");
            devTools.send(Log.enable());

            Map<LogEntry.Level, LogEntry.Source> logs = new HashMap<>();

            logger.info("Добавим слушатель для захвата логов с консоли Level == Error");
            devTools.addListener(Log.entryAdded(), logEntry -> {
                if (logEntry.getLevel().equals(LogEntry.Level.ERROR)) {
//                System.out.println("-------------------------------------------");
//                System.out.println("Level: " + logEntry.getLevel());
//                System.out.println("Request ID: " + logEntry.getNetworkRequestId());
//                System.out.println("URL: " + logEntry.getUrl());
//                System.out.println("Source: " + logEntry.getSource());
//                System.out.println("Text: " + logEntry.getText());
//                System.out.println("Timestamp: " + logEntry.getTimestamp());
//                System.out.println("-------------------------------------------");

                    logs.put(logEntry.getLevel(), logEntry.getSource());
                }
            });

            open(URL);

            assertThat(logs).isEmpty();
        }

        @Test
        void getConsoleLogsTest() {
            String message = "Are logs in console available?";
            devTools.send(Log.enable());

            devTools.addListener(Log.entryAdded(), consoleMessageFromDevTools ->
                    assertEquals(consoleMessageFromDevTools.getText(), message));

            executeJavaScript("console.log('" + message + "');");

        }
    }

    @Nested
    @DisplayName("Headers: Подстановка кастомных хедеров")
    class HeadersTest {

        @ParameterizedTest(name = "{argumentsWithNames}")
        @MethodSource("selenide.devtools.demo.devtoolsTests.DevToolsTest#getArgumentsForHeadersTest")
        //@CsvFileSource(resources = "/data/headers-test-data.csv", delimiter = ',', numLinesToSkip = 1)
        public void addCustomHeaderTest(String headerTitle, Object headerValue) {

            devTools.send(Network.enable(Optional.empty(), Optional.empty(), Optional.empty()));
            devTools.send(Network.setExtraHTTPHeaders(new Headers(ImmutableMap.of(headerTitle, headerValue))));

            Map<String, Headers> headers = new HashMap<>();

            devTools.addListener(Network.requestWillBeSent(), requestWillBeSent -> {
                if (requestWillBeSent.getRequest().getUrl().equals("https://www.imdb.com/")) {
                    headers.put("headers", requestWillBeSent.getRequest().getHeaders());
                }
            });

            open("https://www.imdb.com/");

            System.out.println(headers);

            Headers requestHeaders = headers.get("headers");

            assertThat(requestHeaders.get(headerTitle)).isEqualTo(headerValue);
        }
    }
}
