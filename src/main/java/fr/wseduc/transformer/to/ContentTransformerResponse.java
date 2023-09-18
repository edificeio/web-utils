package fr.wseduc.transformer.to;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.vertx.core.json.JsonObject;

import java.util.Map;
import java.util.Objects;

/**
 * Content transformer service response
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ContentTransformerResponse {

    /**
     * Version of transformed content
     */
    private final int contentVersion;
    /**
     * Html content transformed from json content
     */
    private final String htmlContent;
    /**
     * Json content transformed from html content
     */
    private final JsonObject jsonContent;
    /**
     * Plain text content from transformed content
     */
    private final String plainTextContent;

    /**
     * Constructor
     * @param contentVersion version of transformed content
     * @param htmlContent html content transformed from json
     * @param jsonContent json content transformed from html
     */
    @JsonCreator
    public ContentTransformerResponse(@JsonProperty("contentVersion") int contentVersion,
                                      @JsonProperty("htmlContent") String htmlContent,
                                      @JsonProperty("jsonContent") Map<String, Object> jsonContent,
                                      @JsonProperty("plainTextContent") String plainTextContent) {
        this.contentVersion = contentVersion;
        this.htmlContent = htmlContent;
        this.jsonContent = new JsonObject(jsonContent);
        this.plainTextContent = plainTextContent;
    }

    public int getContentVersion() {
        return contentVersion;
    }

    public String getHtmlContent() {
        return htmlContent;
    }

    public JsonObject getJsonContent() {
        return jsonContent;
    }

    public String getPlainTextContent() {
        return plainTextContent;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ContentTransformerResponse that = (ContentTransformerResponse) o;
        return contentVersion == that.contentVersion && Objects.equals(htmlContent, that.htmlContent) && Objects.equals(jsonContent, that.jsonContent) && Objects.equals(plainTextContent, that.plainTextContent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(contentVersion, htmlContent, jsonContent, plainTextContent);
    }
}
