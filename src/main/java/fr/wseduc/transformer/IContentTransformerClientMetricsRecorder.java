package fr.wseduc.transformer;

import fr.wseduc.transformer.to.ContentTransformerRequest;

public interface IContentTransformerClientMetricsRecorder {
    /**
     * Called when a successful transformation was performed.
     * @param request Transformation request
     * @param durationInMs Duration of the transformation
     */
    void onTransformSuccess(final ContentTransformerRequest request, final long durationInMs);
    /**
     * Called when a transformation raised an error.
     * @param request Transformation request
     * @param durationInMs Duration of the transformation
     */
    void onTransformFailure(final ContentTransformerRequest request, final long durationInMs);

    static final NoopContentTransformerClientMetricsRecorder noop = new NoopContentTransformerClientMetricsRecorder();

    static class NoopContentTransformerClientMetricsRecorder implements IContentTransformerClientMetricsRecorder {

        @Override
        public void onTransformSuccess(final ContentTransformerRequest request, final long durationInMs) {

        }

        @Override
        public void onTransformFailure(final ContentTransformerRequest request, final long durationInMs) {

        }
    }
}
