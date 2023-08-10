package fr.wseduc.transformer;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

/**
 * Holder class for {@link ContentTransformer} instance
 */
public class ContentTransformerHolder {

    /**
     * Instance of {@link ContentTransformer}
     */
    private static ContentTransformer instance;

    /**
     * Method initiating {@link ContentTransformer} instance
     * @param vertx vertx
     * @param config application configuration
     */
    public static void init(Vertx vertx, JsonObject config) {
        if (instance == null) {
            instance = new ContentTransformer(vertx, config);
        } else {
            throw new IllegalStateException("Content transformer instance has already been initialized.");
        }
    }

    /**
     * Method retrieving {@link ContentTransformer} instance
     * @return instance of content transformer
     */
    public static ContentTransformer getInstance() {
        if (instance == null) {
            throw new IllegalStateException("Content transformer instance has not been initialized.");
        } else {
            return instance;
        }
    }
}
