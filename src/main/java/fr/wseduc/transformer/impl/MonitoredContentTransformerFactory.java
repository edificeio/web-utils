package fr.wseduc.transformer.impl;

import fr.wseduc.transformer.IContentTransformerClient;
import fr.wseduc.transformer.IContentTransformerClientMetricsRecorder;
import fr.wseduc.transformer.IContentTransformerFactory;

/**
 * Factory for monitored content transformer client
 */
public class MonitoredContentTransformerFactory implements IContentTransformerFactory {

    private final IContentTransformerFactory contentTransformerFactory;
    private final IContentTransformerClientMetricsRecorder metricsRecorder;

    public MonitoredContentTransformerFactory(final IContentTransformerFactory contentTransformerFactory,
                                              final ContentTransformerClientMetricsRecorder.Configuration configuration) {
        this.contentTransformerFactory = contentTransformerFactory;
        this.metricsRecorder = new ContentTransformerClientMetricsRecorder(configuration);
    }

    @Override
    public IContentTransformerClient create() {
        return new ContentTransformerClientWithMetrics(contentTransformerFactory.create(), metricsRecorder);
    }
}
