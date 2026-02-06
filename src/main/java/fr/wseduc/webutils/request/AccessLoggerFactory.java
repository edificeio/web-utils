package fr.wseduc.webutils.request;

public class AccessLoggerFactory {

    /**
     * Factory method to create the appropriate AccessLogger.
     * @param  format If is set to "json", returns EntAccessLoggerJson (JSON format). Otherwise, returns EntAccessLogger (plain text format).
     * @return AccessLogger instance (either plain text or JSON)
     */
    public static IAccessLogger create() {
        final String format = System.getenv(IAccessLogger.LOG_FORMAT_CONF_KEY);
        if ("json".equalsIgnoreCase(format)) {
            return new AccessLoggerJson();
        } else {
            return new AccessLogger();
        }
    }
}
