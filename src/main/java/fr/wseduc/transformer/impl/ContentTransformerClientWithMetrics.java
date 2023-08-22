package fr.wseduc.transformer.impl;

import fr.wseduc.transformer.IContentTransformerClient;
import fr.wseduc.transformer.to.ContentTransformerRequest;
import fr.wseduc.transformer.to.ContentTransformerResponse;
import io.vertx.core.Future;
import org.apache.commons.lang3.NotImplementedException;

/**
 * Monitored content transformer client
 */
public class ContentTransformerClientWithMetrics implements IContentTransformerClient {
    @Override
    public Future<ContentTransformerResponse> transform(ContentTransformerRequest request) {
        // TODO mest : implement metrics monitoring over content transformer client in WB-2009
        throw new NotImplementedException("Metrics monitoring will be implemented in WB-2009");
    }
}
