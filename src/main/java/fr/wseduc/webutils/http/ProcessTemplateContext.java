package fr.wseduc.webutils.http;

import com.samskivert.mustache.Mustache;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;

import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

/**
 * Container for all parameters needed for the template processinf
 */
public class ProcessTemplateContext {

    private HttpServerRequest request;
    private JsonObject params;
    private String templateString;
    private Reader reader;
    private boolean escapeHtml;
    private String defaultValue;
    private final Map<String, Mustache.Lambda> lambdas = new HashMap<>();

    public ProcessTemplateContext request(HttpServerRequest request) {
        this.request = request;
        return this;
    }

    public ProcessTemplateContext escapeHtml(boolean escapeHtml) {
        this.escapeHtml = escapeHtml;
        return this;
    }

    public ProcessTemplateContext setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }

    public ProcessTemplateContext params(JsonObject params) {
        this.params = params;
        return this;
    }

    public ProcessTemplateContext templateString(String templateString) {
        this.templateString = templateString;
        return this;
    }

    public ProcessTemplateContext reader(Reader reader) {
        this.reader = reader;
        return this;
    }

    public ProcessTemplateContext lambdas(Map<String, Mustache.Lambda> lambdas) {
        this.lambdas.putAll(lambdas);
        return this;
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
