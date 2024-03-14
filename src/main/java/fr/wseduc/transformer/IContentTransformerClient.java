package fr.wseduc.transformer;

import fr.wseduc.transformer.to.ContentTransformerRequest;
import fr.wseduc.transformer.to.ContentTransformerResponse;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServerRequest;

/**
 * Client to call content transformer service
 */
public interface IContentTransformerClient {

    /**
     * Dummy instance of content transformer client
     */
    IContentTransformerClient noop = new NoopContentTransformerClient();

    /**
     * Method calling transformation service
     * @param request request specifying transformation method, content version and content to transform.
     * @param httpCallerRequest incoming HTTP request that required a call to the transformer
     * @return response containing the transformed content
     */
    Future<ContentTransformerResponse> transform(final ContentTransformerRequest request,
                                                 final HttpServerRequest httpCallerRequest);

    /**
     * Dummy implementation of {@link IContentTransformerClient}
     */
    class NoopContentTransformerClient implements IContentTransformerClient {

        @Override
        public Future<ContentTransformerResponse> transform(final ContentTransformerRequest request,
                                                            final HttpServerRequest httpCallerRequest) {
            return Future.succeededFuture();
        }
    }
}
