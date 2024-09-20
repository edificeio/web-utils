package fr.wseduc.transformer;

import fr.wseduc.transformer.to.ContentTransformerRequest;
import fr.wseduc.transformer.to.ContentTransformerResponse;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServerRequest;

/**
 * Client to call content transformer service
 */
@FunctionalInterface
public interface IContentTransformerClient {

    /**
     * Dummy instance of content transformer client
     */
    IContentTransformerClient noop = request -> Future.succeededFuture();

  /**
     * Method calling transformation service
     * @param request request specifying transformation method, content version and content to transform.
     * @return response containing the transformed content
     */
    Future<ContentTransformerResponse> transform(final ContentTransformerRequest request);

}
