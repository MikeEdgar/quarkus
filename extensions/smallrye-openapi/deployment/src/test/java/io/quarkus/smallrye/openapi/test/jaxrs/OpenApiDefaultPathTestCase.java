package io.quarkus.smallrye.openapi.test.jaxrs;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

class OpenApiDefaultPathTestCase {
    private static final String OPEN_API_PATH = "/q/openapi";

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(OpenApiResource.class, ResourceBean.class)
                    .addAsResource(new StringAsset("quarkus.smallrye-openapi.store-schema-directory=target"),
                            "application.properties"));

    @ParameterizedTest
    @CsvSource({
            // YAML default
            "GET, " + OPEN_API_PATH + ", '', '', 'application/yaml;charset=UTF-8'",
            "HEAD, " + OPEN_API_PATH + ", '', '', 'application/yaml;charset=UTF-8'",
            // YAML by Accept header (accepts both, prefers YAML)
            "GET, " + OPEN_API_PATH + ", 'application/json;q=0.9, application/yaml', '', 'application/yaml;charset=UTF-8'",
            "HEAD, " + OPEN_API_PATH + ", 'application/json;q=0.9, application/yaml', '', 'application/yaml;charset=UTF-8'",
            // YAML by Accept header (accepts only YAML)
            "GET, " + OPEN_API_PATH + ", 'application/yaml', '', 'application/yaml;charset=UTF-8'",
            "HEAD, " + OPEN_API_PATH + ", 'application/yaml', '', 'application/yaml;charset=UTF-8'",
            // YAML by Accept header with unsupported value
            "GET, " + OPEN_API_PATH + ", 'text/plain', '', 'application/yaml;charset=UTF-8'",
            "HEAD, " + OPEN_API_PATH + ", 'text/plain', '', 'application/yaml;charset=UTF-8'",
            // YAML by `format` query parameter
            "GET, " + OPEN_API_PATH + ", '', 'YAML', 'application/yaml;charset=UTF-8'",
            "HEAD, " + OPEN_API_PATH + ", '', 'YAML', 'application/yaml;charset=UTF-8'",
            // YAML by file extension
            "GET, " + OPEN_API_PATH + ".yaml, '', '', 'application/yaml;charset=UTF-8'",
            "GET, " + OPEN_API_PATH + ".yml, '', '', 'application/yaml;charset=UTF-8'",
            "HEAD, " + OPEN_API_PATH + ".yaml, '', '', 'application/yaml;charset=UTF-8'",
            "HEAD, " + OPEN_API_PATH + ".yml, '', '', 'application/yaml;charset=UTF-8'",
            // JSON by Accept header (accepts both, JSON first)
            "GET, " + OPEN_API_PATH + ", 'application/json, application/yaml', '', 'application/json;charset=UTF-8'",
            "HEAD, " + OPEN_API_PATH + ", 'application/json, application/yaml', '', 'application/json;charset=UTF-8'",
            // JSON by `format` query parameter
            "GET, " + OPEN_API_PATH + ", '', 'JSON', 'application/json;charset=UTF-8'",
            "HEAD, " + OPEN_API_PATH + ", '', 'JSON', 'application/json;charset=UTF-8'",
            // JSON by file extension
            "GET, " + OPEN_API_PATH + ".json, '', '', 'application/json;charset=UTF-8'",
            "HEAD, " + OPEN_API_PATH + ".json, '', '', 'application/json;charset=UTF-8'",
    })
    void testOpenApiPathContentTypeVariations(String httpMethod, String path, String acceptHeader, String formatParam,
            String expectedContentType) {
        var request = RestAssured.given();

        if (!acceptHeader.isBlank()) {
            request = request.header("Accept", acceptHeader);
        }

        if (!formatParam.isBlank()) {
            request = request.queryParam("format", formatParam);
        }

        var response = request.request(httpMethod, path);

        response.then().header("Content-Type", expectedContentType);

        if ("HEAD".equals(httpMethod)) {
            response.then().body(Matchers.is(Matchers.emptyOrNullString()));
        }
    }

    @Test
    void testOpenApiPathAccessResource() {
        RestAssured.given().queryParam("format", "JSON")
                .when().get(OPEN_API_PATH)
                .then()
                .header("Content-Type", "application/json;charset=UTF-8")
                .body("openapi", Matchers.startsWith("3.1"))
                .body("info.title", Matchers.equalTo("quarkus-smallrye-openapi-deployment API"))
                .body("tags.name[0]", Matchers.equalTo("test"))
                .body("paths.'/resource'.get.servers[0]", Matchers.hasKey("url"))
                .body("paths.'/resource'.get.security[0]", Matchers.hasKey("securityRequirement"))
                .body("paths.'/resource'.get", Matchers.hasKey("x-openApiExtension"));
    }

    @Test
    void testOpenApiOptionsHttpMethod() {
        RestAssured.given()
                .when().options(OPEN_API_PATH)
                .then().header("Allow", "GET, HEAD, OPTIONS")
                .body(Matchers.is(Matchers.emptyOrNullString()));
    }

    @Test
    void testOpenApiUnsupportedHttpMethod() {
        RestAssured.given()
                .when().delete(OPEN_API_PATH)
                .then().statusCode(405);
    }
}
