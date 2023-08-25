package fr.wseduc.transformer.impl;

import fr.wseduc.transformer.IContentTransformerClient;
import fr.wseduc.transformer.IContentTransformerClientMetricsRecorder;
import fr.wseduc.transformer.to.ContentTransformerRequest;
import fr.wseduc.transformer.to.ContentTransformerResponse;
import io.vertx.core.Future;

/**
 * Monitored content transformer client
 */
public class ContentTransformerClientWithMetrics implements IContentTransformerClient {

    private final IContentTransformerClient client;
    private final IContentTransformerClientMetricsRecorder metricsRecorder;

    public ContentTransformerClientWithMetrics(final IContentTransformerClient client,
                                               final IContentTransformerClientMetricsRecorder metricsRecorder) {
        this.client = client;
        this.metricsRecorder = metricsRecorder;
    }

    @Override
    public Future<ContentTransformerResponse> transform(ContentTransformerRequest request) {
        final long start = System.currentTimeMillis();
        return client.transform(request).onSuccess(sent ->
                metricsRecorder.onTransformSuccess(request.getAction(), System.currentTimeMillis() - start)
        ).onFailure(th ->
                metricsRecorder.onTransformFailure(request.getAction(), System.currentTimeMillis() - start)
        );
    }
}
