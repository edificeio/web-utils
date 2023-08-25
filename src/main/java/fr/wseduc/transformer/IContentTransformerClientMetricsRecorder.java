package fr.wseduc.transformer;

import fr.wseduc.transformer.to.ContentTransformerAction;

public interface IContentTransformerClientMetricsRecorder {
    /**
     * Called when a successful transformation was performed.
     * @param action Transformation performed
     * @param durationInMs Duration of the transformation
     */
    void onTransformSuccess(final ContentTransformerAction action, final long durationInMs);
    /**
     * Called when a transformation raised an error.
     * @param action Transformation performed
     * @param durationInMs Duration of the transformation
     */
    void onTransformFailure(final ContentTransformerAction action, final long durationInMs);

    static final NoopContentTransformerClientMetricsRecorder noop = new NoopContentTransformerClientMetricsRecorder();

    static class NoopContentTransformerClientMetricsRecorder implements IContentTransformerClientMetricsRecorder {

        @Override
        public void onTransformSuccess(final ContentTransformerAction action, final long durationInMs) {

        }

        @Override
        public void onTransformFailure(final ContentTransformerAction action, final long durationInMs) {

        }
    }
}
