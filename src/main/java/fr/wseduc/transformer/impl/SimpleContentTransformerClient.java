package fr.wseduc.transformer.impl;

import fr.wseduc.transformer.IContentTransformerClient;
import fr.wseduc.transformer.to.ContentTransformerRequest;
import fr.wseduc.transformer.to.ContentTransformerResponse;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.http.impl.headers.HeadersMultiMap;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

/**
 * Client to call rich content transformer service
 */
public class SimpleContentTransformerClient implements IContentTransformerClient {

    private static final Logger log = LoggerFactory.getLogger(SimpleContentTransformerClient.class);
    private final HttpClient httpClient;
    private final String authHeader;

    /**
     * @param httpClient HTTP client
     * @param authHeader Authorization header to authorize calls to content transformer. May be null if we
     *                   don't use any form of authentication
     */
    public SimpleContentTransformerClient(final HttpClient httpClient, final String authHeader) {
        this.httpClient = httpClient;
        this.authHeader = authHeader;
    }

    /**
     * Method calling rich content transformer service
     * @param contentTransformerRequest a request containing the content to transform
     * @return the response containing the transformed content
     */
    public Future<ContentTransformerResponse> transform(final ContentTransformerRequest contentTransformerRequest) {
        final Promise<ContentTransformerResponse> promise = Promise.promise();
        HeadersMultiMap headers = new HeadersMultiMap().add("Content-Type", "application/json");
        if(isNotEmpty(authHeader)) {
            headers.add("Authorization", authHeader);
        }
        httpClient.request(new RequestOptions()
                .setMethod(HttpMethod.POST)
                .setURI("/transform")
                .setHeaders(headers))
                .flatMap(request -> request.send(JsonObject.mapFrom(contentTransformerRequest).encode()))
                .onSuccess(response -> {
                    final int statusCode = response.statusCode();
                    if (statusCode == 200) {
                        response.bodyHandler(body -> promise.complete(Json.decodeValue(body, ContentTransformerResponse.class)));
                    } else {
                      promise.fail("transform.error." + statusCode);
                      response.bodyHandler( body -> log.warn(createReport(statusCode, contentTransformerRequest, body.toString())));
                    }
                })
                .onFailure(th -> {
                  promise.fail("transform.error.500");
                  log.warn(createReport(500, contentTransformerRequest, th.getMessage()));
                });
        return promise.future();
    }

    private String createReport(final int statusCode, final ContentTransformerRequest contentTransformerRequest,
                                final String body) {
        final JsonObject report = new JsonObject()
          .put("httpcode", statusCode)
          .put("payload", Json.encode(contentTransformerRequest));
        if(body != null) {
          report.put("response", body);
        }
        return Json.encode(report);
    }
}
