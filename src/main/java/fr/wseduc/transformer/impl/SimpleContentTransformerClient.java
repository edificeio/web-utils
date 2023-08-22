package fr.wseduc.transformer.impl;

import fr.wseduc.transformer.IContentTransformerClient;
import fr.wseduc.transformer.to.ContentTransformerRequest;
import fr.wseduc.transformer.to.ContentTransformerResponse;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Client to call rich content transformer service
 */
public class SimpleContentTransformerClient implements IContentTransformerClient {

    private static final Logger log = LoggerFactory.getLogger(SimpleContentTransformerClient.class);
    private final HttpClient httpClient;

    /**
     * Constructor
     * @param vertx vertx instance
     * @param config content transformer config
     */
    public SimpleContentTransformerClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    /**
     * Method calling rich content transformer service
     * @param contentTransformerRequest a request containing the content to transform
     * @return the response containing the transformed content
     */
    public Future<ContentTransformerResponse> transform(ContentTransformerRequest contentTransformerRequest) {
        final Promise<ContentTransformerResponse> promise = Promise.promise();
        final HttpClientRequest request = this.httpClient.post("/transform", response -> {
            if (response.statusCode() == 200) {
                response.bodyHandler(body -> promise.complete(Json.decodeValue(body, ContentTransformerResponse.class)));
            } else {
                response.bodyHandler( body -> promise.fail("transform.error." + response.statusCode() + " with response body: " + body.toString()));
            }
        });
        request.exceptionHandler(promise::fail);
        request.putHeader("Content-Type", "application/json");
        request.end(JsonObject.mapFrom(contentTransformerRequest).encode());
        return promise.future();
    }
}
