package fr.wseduc.transformer.impl;

import fr.wseduc.transformer.IContentTransformerClient;
import fr.wseduc.transformer.IContentTransformerFactory;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.net.ClientOptionsBase;
import io.vertx.core.net.TCPSSLOptions;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Simple implementation of content transformer factory
 */
public class SimpleContentTransformerFactory implements IContentTransformerFactory {
    private final Logger logger = LoggerFactory.getLogger(SimpleContentTransformerFactory.class);
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
            .setDefaultPort(uri.getPort())
            .setMaxPoolSize(contentTransformerConfig.getInteger("pool", 16))
            .setKeepAlive(contentTransformerConfig.getBoolean("keepalive", true))
            .setConnectTimeout(contentTransformerConfig.getInteger("timeout", ClientOptionsBase.DEFAULT_CONNECT_TIMEOUT))
            .setIdleTimeout(contentTransformerConfig.getInteger("idle-timeout", TCPSSLOptions.DEFAULT_IDLE_TIMEOUT))
            .setKeepAliveTimeout(contentTransformerConfig.getInteger("keepalive-timeout", HttpClientOptions.DEFAULT_KEEP_ALIVE_TIMEOUT))
            .setHttp2KeepAliveTimeout(contentTransformerConfig.getInteger("keepalive-timeout", HttpClientOptions.DEFAULT_HTTP2_KEEP_ALIVE_TIMEOUT));
        final String auth = contentTransformerConfig.getString("auth");
        final String authHeader;
        if(isEmpty(auth)) {
            logger.warn("Created content transformer client without authentication header");
            authHeader = null;
        } else {
            authHeader = "Basic " + auth;
        }
        return new SimpleContentTransformerClient(vertx.createHttpClient(options), authHeader);
    }
}
