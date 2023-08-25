package fr.wseduc.transformer;

import fr.wseduc.transformer.impl.ContentTransformerClientMetricsRecorder;
import fr.wseduc.transformer.impl.NoopContentTransformerFactory;
import fr.wseduc.transformer.impl.MonitoredContentTransformerFactory;
import fr.wseduc.transformer.impl.SimpleContentTransformerFactory;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Class providing implementations of {@link IContentTransformerFactory} upon context
 */
public class ContentTransformerFactoryProvider {

    private static Vertx vertx;

    private static final Logger log = LoggerFactory.getLogger(ContentTransformerFactoryProvider.class);

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
    public static IContentTransformerFactory getFactory(final String appName, JsonObject contentTransformerConfig) {
        if(vertx == null ) {
            throw new IllegalArgumentException("content.transformer.factory.provider.missing.vertx");
        }
        final IContentTransformerFactory contentTransformerFactory;
        if (contentTransformerConfig == null) {
            contentTransformerFactory = NoopContentTransformerFactory.instance;
            log.warn("No content transformer configured so rich content won't be synced");
        } else {
            final IContentTransformerFactory innerFactory;
            innerFactory = new SimpleContentTransformerFactory(vertx, contentTransformerConfig);
            final JsonObject metricsOptions = contentTransformerConfig.getJsonObject("content-transformer-metrics", new JsonObject());
            if (metricsOptions.getBoolean("enabled", false)) {
                contentTransformerFactory = new MonitoredContentTransformerFactory(
                        innerFactory,
                        ContentTransformerClientMetricsRecorder.Configuration.fromJson(appName, metricsOptions));
            } else {
                contentTransformerFactory = innerFactory;
            }
        }
        return contentTransformerFactory;
    }
}
