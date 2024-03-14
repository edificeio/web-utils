package fr.wseduc.transformer.impl;

import fr.wseduc.transformer.IContentTransformerClient;
import fr.wseduc.transformer.IContentTransformerClientMetricsRecorder;
import fr.wseduc.transformer.to.ContentTransformerRequest;
import fr.wseduc.transformer.to.ContentTransformerResponse;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServerRequest;

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
    public Future<ContentTransformerResponse> transform(ContentTransformerRequest request,
                                                        final HttpServerRequest httpCallerRequest) {
        final long start = System.currentTimeMillis();
        return client.transform(request, httpCallerRequest).onSuccess(sent ->
                metricsRecorder.onTransformSuccess(request, System.currentTimeMillis() - start)
        ).onFailure(th ->
                metricsRecorder.onTransformFailure(request, System.currentTimeMillis() - start)
        );
    }
}
