package fr.wseduc.webutils.http;

import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.spi.context.storage.ContextLocal;

import java.util.UUID;

import static fr.wseduc.webutils.Controller.TRACE_ID;

/**
 * Handle trace id management
 */
public class TraceIdContextHandler {

    public static String getTraceId(Context context, HttpServerRequest request) {
        try {
           if(context.getLocal(TRACE_ID) == null) {
               String traceId = request.getHeader(TRACE_ID);
               if(traceId == null) {
                   traceId = UUID.randomUUID().toString();
               }
               context.putLocal(TRACE_ID, traceId);
            }
            return context.getLocal(TRACE_ID);
        } catch (IllegalArgumentException e) {
            //we are out of the context, can happen with endHandler or worker if we don't join the context
            return "";
        }
    }

}
