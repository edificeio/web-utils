package fr.wseduc.transformer.impl;

import fr.wseduc.transformer.IContentTransformerClient;
import fr.wseduc.transformer.IContentTransformerFactory;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.json.JsonObject;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Simple implementation of content transformer factory
 */
public class SimpleContentTransformerFactory implements IContentTransformerFactory {
    private final Vertx vertx;

    private final JsonObject contentTransformerConfig;

    public SimpleContentTransformerFactory(Vertx vertx, JsonObject contentTransformerConfig) {
        this.vertx = vertx;
        this.contentTransformerConfig = contentTransformerConfig;
    }

    @Override
    public IContentTransformerClient create() {
        final URI uri;
        try {
            uri = new URI(contentTransformerConfig.getString("url"));
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Failed parsing content transformer service url", e);
        }
        final HttpClientOptions options = new HttpClientOptions()
                .setDefaultHost(uri.getHost())
                .setDefaultPort(uri.getPort());
        return new SimpleContentTransformerClient(vertx.createHttpClient(options));
    }
}
