package io.quarkiverse.loggingui.quarkus.logging.ui.deployment;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

@SuppressWarnings("unchecked")
class CustomEndpointTest {
    private static final String LOGGER_PREFIX = "Test2";
    private static final String EXTENSION_ENDPOINT = "myownendpoint/";

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.logging-ui.base-path", EXTENSION_ENDPOINT);

    @Test
    public void getEndpointWithoutTrailingSlashListsLoggersAndLevels() {
        String endpointWithoutTrailingSlash = EXTENSION_ENDPOINT.substring(0, EXTENSION_ENDPOINT.lastIndexOf("/"));
        Map<String, Object> response = RestAssured.when().get(endpointWithoutTrailingSlash).then().statusCode(200)
                .body("loggers", notNullValue())
                .body("levels", notNullValue())
                .extract()
                .as(Map.class);

        ArrayList<String> levels = (ArrayList) response.get("levels");
        assertThat(levels.size(), equalTo(9));
        Map<String, Map<String, String>> descriptionByLoggerName = (Map<String, Map<String, String>>) response.get(
                "loggers");
        assertThat(descriptionByLoggerName.size(), greaterThan(1));
    }

    @Test
    public void getEndpointListsLoggersAndLevels() {
        Map<String, Object> response = performGetRequest("");
        ArrayList<String> levels = (ArrayList) response.get("levels");
        assertThat(levels.size(), equalTo(9));
        Map<String, Map<String, String>> descriptionByLoggerName = (Map<String, Map<String, String>>) response.get(
                "loggers");
        assertThat(descriptionByLoggerName.size(), greaterThan(1));
    }

    @Test
    public void getEndpointListSpecificLogger() {
        String loggerName = createLoggerName("io.quarkus");
        Map<String, Object> response = performGetRequest(loggerName);
        checkGetResponse(response, loggerName);
    }

    @Test
    public void getEndpointListROOTLogger() {
        String loggerName = createLoggerName("ROOT");
        Map<String, Object> response = performGetRequest(loggerName);
        checkGetResponse(response, loggerName);
    }

    @Test
    public void postEndpointSetsLogLevel() {
        String loggerName = createLoggerName("ROOT");
        String newLogLevel = "TRACE";
        Map<String, Object> response = performGetRequest(loggerName);
        checkGetResponse(response, loggerName);
        performPostRequest(loggerName, newLogLevel, 204);
        Map<String, Object> secondCallResponse = performGetRequest(loggerName);
        checkGetResponse(secondCallResponse, loggerName, newLogLevel, newLogLevel);
    }

    @Test
    public void postEndpointSetsLogLevelAlsoInLowerCase() {
        String loggerName = createLoggerName("io.quarkus.loggers");
        String newLogLevel = "trace";
        Map<String, Object> response = performGetRequest(loggerName);
        checkGetResponse(response, loggerName);
        performPostRequest(loggerName, newLogLevel, 204);
        Map<String, Object> secondCallResponse = performGetRequest(loggerName);
        checkGetResponse(secondCallResponse, loggerName, newLogLevel.toUpperCase(Locale.ROOT),
                newLogLevel.toUpperCase(Locale.ROOT));
    }

    private String createLoggerName(String name) {
        return LOGGER_PREFIX + name;
    }

    private Map<String, Object> performGetRequest(String loggerName) {
        return RestAssured.when().get(EXTENSION_ENDPOINT + loggerName).then().statusCode(200)
                .body("loggers", notNullValue())
                .body("levels", notNullValue())
                .extract()
                .as(Map.class);
    }

    private void checkGetResponse(Map<String, Object> response, String loggerName) {
        checkGetResponse(response, loggerName, null, "INFO");
    }

    private void checkGetResponse(Map<String, Object> response, String loggerName, String configuredLogLevel,
            String effectiveLogLevel) {
        ArrayList<String> levels = (ArrayList) response.get("levels");
        assertThat(levels.size(), equalTo(9));

        Map<String, Map<String, String>> descriptionByLoggerName = (Map<String, Map<String, String>>) response.get(
                "loggers");
        assertThat(descriptionByLoggerName.size(), equalTo(1));
        Map<String, String> logLevelDescription = descriptionByLoggerName.get(loggerName);
        assertThat(logLevelDescription.get("configuredLevel"),
                configuredLogLevel == null ? nullValue() : equalTo(configuredLogLevel));
        assertThat(logLevelDescription.get("effectiveLevel"), equalTo(effectiveLogLevel));
    }

    @Test
    public void postEndpointFailsOnUnknownLogLevel() {
        String loggerName = "ROOT";
        String newLogLevel = "non-existing";
        performPostRequest(loggerName, newLogLevel, 400);
    }

    private void performPostRequest(String loggerName, String newLogLevel, int expectedStatusCode) {
        given().contentType("application/json")
                .body(String.format("{\"configuredLevel\": \"%s\"}", newLogLevel))
                .when()
                .post(EXTENSION_ENDPOINT + "{loggerName}", loggerName)
                .then().statusCode(expectedStatusCode);
    }
}
