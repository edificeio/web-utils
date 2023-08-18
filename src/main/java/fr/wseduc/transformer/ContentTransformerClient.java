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

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Client to call rich content transformer service
 */
public class ContentTransformerClient {

    private static final Logger log = LoggerFactory.getLogger(ContentTransformerClient.class);
    private final HttpClient httpClient;

    /**
     * Constructor
     * @param vertx vertx instance
     * @param config content transformer config
     */
    private ContentTransformerClient(Vertx vertx, JsonObject config) throws URISyntaxException {
        final URI uri = new URI(config.getString("url"));
        final HttpClientOptions options = new HttpClientOptions()
                .setDefaultHost(uri.getHost())
                .setDefaultPort(uri.getPort());
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
                response.bodyHandler(body -> promise.complete(new JsonObject(body.toString()).mapTo(ContentTransformerResponse.class)));
            } else {
                promise.fail("transform.error." + response.statusCode());
            }
        });
        request.exceptionHandler(promise::fail);
        request.putHeader("Content-Type", "application/json");
        request.end(JsonObject.mapFrom(contentTransformerRequest).encode());
        return promise.future();
    }

    /**
     * Holder for {@link ContentTransformerClient} instance
     */
    public static class Holder {

        /**
         * Instance of {@link ContentTransformerClient}
         */
        private static ContentTransformerClient instance;

        /**
         * Method initiating {@link ContentTransformerClient} instance
         * @param vertx vertx
         * @param config application configuration
         */
        public static void init(Vertx vertx, JsonObject config) throws URISyntaxException {
            if (instance == null) {
                instance = new ContentTransformerClient(vertx, config);
            } else {
                throw new IllegalStateException("Content transformer instance has already been initialized.");
            }
        }

        /**
         * Method retrieving {@link ContentTransformerClient} instance
         * @return instance of content transformer client
         */
        public static ContentTransformerClient getInstance() {
            if (instance == null) {
                throw new IllegalStateException("Content transformer instance has not been initialized.");
            } else {
                return instance;
            }
        }
    }
}
