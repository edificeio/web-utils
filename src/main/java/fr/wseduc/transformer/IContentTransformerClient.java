package fr.wseduc.transformer;

import fr.wseduc.transformer.to.ContentTransformerRequest;
import fr.wseduc.transformer.to.ContentTransformerResponse;
import io.vertx.core.Future;

/**
 * Client to call content transformer service
 */
public interface IContentTransformerClient {

    /**
     * Dummy instance of content transformer client
     */
    static final IContentTransformerClient noop = new NoopContentTransformerClient();

    /**
     * Method calling transformation service
     * @param request request specifying transformation method, content version and content to transform.
     * @return response containing the transformed content
     */
    Future<ContentTransformerResponse> transform(ContentTransformerRequest request);

    /**
     * Dummy implementation of {@link IContentTransformerClient}
     */
    public static class NoopContentTransformerClient implements IContentTransformerClient {

        @Override
        public Future<ContentTransformerResponse> transform(ContentTransformerRequest request) {
            return Future.succeededFuture();
        }
    }
}
