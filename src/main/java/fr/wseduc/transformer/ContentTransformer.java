package fr.wseduc.transformer;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Client to call rich content transformer service
 */
public class ContentTransformer {

    private static final Logger log = LoggerFactory.getLogger(ContentTransformer.class);
    private final Vertx vertx;
    private final HttpClient httpClient;

    /**
     * Constructor
     * @param vertx vertx instance
     * @param conf configuration for parametrization
     */
    public ContentTransformer(Vertx vertx, JsonObject conf) {
        this.vertx = vertx;
        final HttpClientOptions options = new HttpClientOptions()
                .setDefaultHost("localhost")
                .setDefaultPort(3100);
        this.httpClient = vertx.createHttpClient(options);
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
                response.bodyHandler(body -> {
                    promise.complete(new JsonObject(body.toString()).mapTo(ContentTransformerResponse.class));
                });
            } else {
                promise.fail("transform.error." + response.statusCode());
            }
        });
        request.exceptionHandler(promise::fail);
        request.putHeader("Content-Type", "application/json");
        request.end(JsonObject.mapFrom(contentTransformerRequest).encode());
        return promise.future();
    }
}
