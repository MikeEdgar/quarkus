package io.quarkus.smallrye.openapi.runtime;

import io.quarkus.arc.Arc;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;

/**
 * Handler that serve the OpenAPI document in either json or yaml format
 */
public class OpenApiHandler implements Handler<RoutingContext> {

    private static final String ALLOWED_METHODS = "GET, HEAD, OPTIONS";
    private static final String QUERY_PARAM_FORMAT = "format";
    private static final String MIME_TYPE_JSON = "application/json";
    private static final String MIME_TYPE_YAML = "application/yaml";

    private volatile OpenApiDocumentService openApiDocumentService;

    @Override
    public void handle(RoutingContext event) {
        HttpServerRequest req = event.request();
        HttpServerResponse resp = event.response();
        HttpMethod method = req.method();

        if (method.equals(HttpMethod.OPTIONS)) {
            resp.headers().set(HttpHeaders.ALLOW, ALLOWED_METHODS);
            resp.setStatusCode(204);
        } else if (method.equals(HttpMethod.HEAD) || method.equals(HttpMethod.GET)) {
            String mimeType;

            if (jsonAccepted(event) || jsonRequested(event)) {
                mimeType = MIME_TYPE_JSON;
            } else {
                // Default content type is YAML
                mimeType = MIME_TYPE_YAML;
            }

            resp.headers().set(HttpHeaders.CONTENT_TYPE, mimeType + ";charset=UTF-8");
            byte[] schemaDocument = getOpenApiDocumentService().getDocument(mimeType);
            resp.headers().set(HttpHeaders.CONTENT_LENGTH, Integer.toString(schemaDocument.length));

            if (method.equals(HttpMethod.GET)) {
                resp.write(Buffer.buffer(schemaDocument));
            }
        } else {
            resp.setStatusCode(405);
        }

        resp.end();
    }

    private boolean jsonAccepted(RoutingContext event) {
        // Content negotiation with Accept header
        for (var accept : event.parsedHeaders().accept()) {
            if (MIME_TYPE_JSON.equals(accept.value())) {
                return true;
            }
        }

        return false;
    }

    private boolean jsonRequested(RoutingContext event) {
        String path = event.normalizedPath();
        return path.endsWith(".json") || "JSON".equalsIgnoreCase(event.queryParams().get(QUERY_PARAM_FORMAT));
    }

    private OpenApiDocumentService getOpenApiDocumentService() {
        if (this.openApiDocumentService == null) {
            this.openApiDocumentService = Arc.container().instance(OpenApiDocumentService.class).get();
        }
        return this.openApiDocumentService;
    }
}
