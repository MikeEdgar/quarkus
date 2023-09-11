package io.quarkus.resteasy.reactive.server.test.customexceptions;

import static io.quarkus.resteasy.reactive.server.test.ExceptionUtil.removeStackTrace;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.ArcUndeclaredThrowableException;
import io.quarkus.resteasy.reactive.server.test.ExceptionUtil;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class UnwrappedExceptionTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(ExceptionResource.class, ExceptionMappers.class, ExceptionUtil.class)
                            .add(new StringAsset("quarkus.rest.unwrap-completion-exception=true"), "application.properties");
                }
            });

    @Test
    public void testWrapperWithUnmappedException() {
        RestAssured.get("/hello/wrapperOfIAE")
                .then().statusCode(500);
    }

    @Test
    public void testWrapperWithMappedException() {
        RestAssured.get("/hello/wrapperOfISE")
                .then().statusCode(999);
    }

    @Test
    public void testUnmappedException() {
        RestAssured.get("/hello/iae")
                .then().statusCode(500);
    }

    @Test
    public void testMappedException() {
        RestAssured.get("/hello/ise")
                .then().statusCode(999);
    }

    @Test
    public void testAsyncWrapperWithUnmappedException() {
        RestAssured.get("/hello/asyncWrapperOfIAE")
                .then().statusCode(500);
    }

    @Test
    public void testAsyncWrapperWithMappedException() {
        RestAssured.get("/hello/asyncWrapperOfISE")
                .then().statusCode(999);
    }

    @Path("hello")
    public static class ExceptionResource {

        @Path("wrapperOfIAE")
        public String wrapperOfIAE() {
            throw removeStackTrace(new ArcUndeclaredThrowableException(removeStackTrace(new IllegalArgumentException())));
        }

        @Path("wrapperOfISE")
        public String wrapperOfISE() {
            throw removeStackTrace(new ArcUndeclaredThrowableException(removeStackTrace(new IllegalStateException())));
        }

        @Path("asyncWrapperOfIAE")
        public CompletionStage<String> asyncWrapperOfIAE() {
            return CompletableFuture.failedStage(removeStackTrace(new CompletionException(removeStackTrace(new IllegalArgumentException()))));
        }

        @Path("asyncWrapperOfISE")
        public CompletionStage<String> asyncWrapperOfISE() {
            return CompletableFuture.failedStage(removeStackTrace(new CompletionException(removeStackTrace(new IllegalStateException()))));
        }

        @Path("iae")
        public String iae() {
            throw removeStackTrace(new IllegalArgumentException());
        }

        @Path("ise")
        public String ise() {
            throw removeStackTrace(new IllegalStateException());
        }
    }

    public static class ExceptionMappers {

        @ServerExceptionMapper
        Response mapISE(IllegalStateException e) {
            return Response.status(999).build();
        }
    }
}
