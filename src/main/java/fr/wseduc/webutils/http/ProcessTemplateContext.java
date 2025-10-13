package fr.wseduc.webutils.http;

import com.samskivert.mustache.Mustache;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;

import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

/**
 * Container for all parameters needed for the template processing
 */
public final class ProcessTemplateContext {

    private final HttpServerRequest request;
    private final JsonObject params;
    private final String templateString;
    private final Reader reader;
    private final boolean escapeHtml;
    private final String defaultValue;
    private final Map<String, Mustache.Lambda> lambdas;

    private ProcessTemplateContext(HttpServerRequest request, JsonObject params, String templateString, Reader reader,
                                  boolean escapeHtml, String defaultValue, Map<String, Mustache.Lambda> lambdas) {
        this.request = request;
        this.params = params;
        this.templateString = templateString;
        this.reader = reader;
        this.escapeHtml = escapeHtml;
        this.defaultValue = defaultValue;
        this.lambdas = new HashMap<>(lambdas);
    }

    public static class Builder {

        private HttpServerRequest request;
        private JsonObject params;
        private String templateString;
        private Reader reader;
        private boolean escapeHtml;
        private String defaultValue;
        private final Map<String, Mustache.Lambda> lambdas = new HashMap<>();

        public Builder request(HttpServerRequest request) {
            this.request = request;
            return this;
        }

        public Builder escapeHtml(boolean escapeHtml) {
            this.escapeHtml = escapeHtml;
            return this;
        }

        public Builder setDefaultValue(String defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public Builder params(JsonObject params) {
            this.params = params;
            return this;
        }

        public Builder templateString(String templateString) {
            this.templateString = templateString;
            return this;
        }

        public Builder reader(Reader reader) {
            this.reader = reader;
            return this;
        }

        public Builder lambdas(Map<String, Mustache.Lambda> lambdas) {
            this.lambdas.putAll(lambdas);
            return this;
        }

        public HttpServerRequest request() {
            return request;
        }

        public ProcessTemplateContext build() {
            return new ProcessTemplateContext(request, params, templateString, reader, escapeHtml, defaultValue, lambdas);
        }

    }

    public HttpServerRequest request() {
        return request;
    }

    public JsonObject params() {
        return params;
    }

    public String templateString() {
        return templateString;
    }

    public Reader reader() {
        return reader;
    }

    public Map<String, Mustache.Lambda> lambdas() {
        return lambdas;
    }

    public boolean escapeHtml() {
        return escapeHtml;
    }

    public String defaultValue() {
        return defaultValue;
    }
}
