package io.quarkus.smallrye.openapi.runtime;

import java.util.Set;

import io.quarkus.arc.Arc;
import io.smallrye.openapi.runtime.io.Format;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.MIMEHeader;
import io.vertx.ext.web.RoutingContext;

/**
 * Handler that serve the OpenAPI document in either json or yaml format
 */
public class OpenApiHandler implements Handler<RoutingContext> {

    private static final String ALLOWED_METHODS = "GET, HEAD, OPTIONS";
    private static final String QUERY_PARAM_FORMAT = "format";
    private static final Set<String> SUPPORTED_MIMETYPES = Set.of(Format.JSON.getMimeType(), Format.YAML.getMimeType());

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
            Format format;

            if (jsonAccepted(event) || jsonRequested(event)) {
                format = Format.JSON;
            } else {
                // Default content type is YAML
                format = Format.YAML;
            }

            resp.headers().set(HttpHeaders.CONTENT_TYPE, format.getMimeType() + ";charset=UTF-8");
            byte[] schemaDocument = getOpenApiDocumentService().getDocument(format);
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
        return event.parsedHeaders()
                .accept()
                .stream()
                .map(MIMEHeader::value)
                .filter(SUPPORTED_MIMETYPES::contains)
                .findFirst()
                .orElseGet(Format.YAML::getMimeType)
                .equals(Format.JSON.getMimeType());
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
