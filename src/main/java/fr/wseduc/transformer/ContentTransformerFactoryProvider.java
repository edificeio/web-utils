package fr.wseduc.transformer;

import fr.wseduc.transformer.impl.NoopContentTransformerFactory;
import fr.wseduc.transformer.impl.MonitoredContentTransformerFactory;
import fr.wseduc.transformer.impl.SimpleContentTransformerFactory;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

/**
 * Class providing implementations of {@link IContentTransformerFactory} upon context
 */
public class ContentTransformerFactoryProvider {

    private static Vertx vertx;

    private ContentTransformerFactoryProvider() {};

    /**
     * Initialization method, must be called before getting a factory
     * @param vertx vertx instance
     */
    public static void init(final Vertx vertx) {
        ContentTransformerFactoryProvider.vertx = vertx;
    }

    /**
     * Gets a factory according to the context specified in the configuration
     * @param contentTransformerConfig the content transformer client configuration
     * @return the content transformer client factory
     */
    public static IContentTransformerFactory getFactory(JsonObject contentTransformerConfig) {
        if(vertx == null ) {
            throw new IllegalArgumentException("content.transformer.factory.provider.missing.vertx");
        }
        final IContentTransformerFactory contentTransformerFactory;
        if (contentTransformerConfig == null) {
            contentTransformerFactory = NoopContentTransformerFactory.instance;
        } else {
            final IContentTransformerFactory innerFactory;
            innerFactory = new SimpleContentTransformerFactory(vertx, contentTransformerConfig);
            final JsonObject metricsOptions = contentTransformerConfig.getJsonObject("content-transformer-metrics", new JsonObject());
            if (metricsOptions.getBoolean("enabled", false)) {
                contentTransformerFactory = new MonitoredContentTransformerFactory(innerFactory);
            } else {
                contentTransformerFactory = innerFactory;
            }
        }
        return contentTransformerFactory;
    }
}
