package fr.wseduc.webutils.http;

import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.spi.context.storage.ContextLocal;
import org.apache.commons.lang3.time.StopWatch;

import java.util.UUID;

import static fr.wseduc.webutils.Controller.TRACE_ID;
import static fr.wseduc.webutils.http.Renders.log;

/**
 * Handle trace id management
 */
public class TraceIdContextHandler {

    private static final String TRACE_TIME = "X-Cloud-Trace-Context-Time";

    /**
     * @return The current registered traceId in vertx's context.
     */
    public static String getTraceId() {
        return getTraceId(Vertx.currentContext(), null);
    }
    public static String getTraceId(Context context, HttpServerRequest request) {
        if (context == null) {
            if(request == null) {
                return "";
            }
            String traceId = request.getHeader(TRACE_ID);
            return traceId != null ? traceId : "";
        }
        try {
           if(context.getLocal(TRACE_ID) == null) {
               String traceId = request == null ? null : request.getHeader(TRACE_ID);
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

    /**
     * Sets a traceId for the current vertx context.
     * @param traceId TraceId to set
     */
    public static void setTraceId(String traceId) {
        setTraceId(Vertx.currentContext(), traceId);
    }

    /**
     * Sets a traceId for the current vertx context.
     * @param context Current vertx context
     * @param traceId Traceid to set
     */
    public static void setTraceId(Context context, String traceId) {
        if(context == null) {
            log.warn("Cannot set trace id on a null context");
        } else {
            context.putLocal(TRACE_ID, traceId);
        }
    }

    public static void setTraceTime(Context context) {
        try {
            if(context.getLocal(TRACE_TIME) == null) {
                context.putLocal(TRACE_TIME, StopWatch.createStarted());
            }
        } catch (IllegalArgumentException e) {
            //we are out of the context, can happen with endHandler or worker if we don't join the context
        }
    }


    public static StopWatch getTraceTime(Context context) {
        try {
            return context.getLocal(TRACE_TIME);
        } catch (IllegalArgumentException e) {
            //we are out of the context, can happen with endHandler or worker if we don't join the context
            return null;
        }
    }

}
