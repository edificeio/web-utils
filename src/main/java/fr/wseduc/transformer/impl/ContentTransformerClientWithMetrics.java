package fr.wseduc.transformer.impl;

import fr.wseduc.transformer.IContentTransformerClient;
import fr.wseduc.transformer.to.ContentTransformerRequest;
import fr.wseduc.transformer.to.ContentTransformerResponse;
import io.vertx.core.Future;

/**
 * Monitored content transformer client
 */
public class ContentTransformerClientWithMetrics implements IContentTransformerClient {
    @Override
    public Future<ContentTransformerResponse> transform(ContentTransformerRequest request) {
        // TODO mest : implement metrics monitoring over content transformer client in WB-2009
        return Future.succeededFuture();
    }
}
